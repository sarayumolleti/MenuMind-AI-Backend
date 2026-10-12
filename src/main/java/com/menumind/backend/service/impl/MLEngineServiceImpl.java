package com.menumind.backend.service.impl;

import com.menumind.backend.entity.*;
import com.menumind.backend.exception.ResourceNotFoundException;
import com.menumind.backend.repository.AIInsightRepository;
import com.menumind.backend.repository.DemandForecastRepository;
import com.menumind.backend.repository.MenuItemRepository;
import com.menumind.backend.repository.OrderItemRepository;
import com.menumind.backend.service.MLEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MLEngineServiceImpl implements MLEngineService {

    private final DemandForecastRepository demandForecastRepository;
    private final AIInsightRepository aiInsightRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;

    private static final int HISTORICAL_DAYS_FOR_FORECAST = 30;
    private static final int HISTORICAL_DAYS_FOR_ANOMALY = 14;

    @Override
    @Transactional
    public List<DemandForecast> generateDemandForecast(Long menuItemId, int forecastDays) {
        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", menuItemId));

        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusDays(HISTORICAL_DAYS_FOR_FORECAST).atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);

        // Fetch raw sales data
        List<Object[]> rawSales = orderItemRepository.findSalesByMenuItemBetween(menuItemId, start, end);
        
        // Aggregate by date to avoid JDBC timezone issues
        Map<LocalDate, Integer> dailyMap = new TreeMap<>();
        for (Object[] row : rawSales) {
            LocalDateTime dt = (LocalDateTime) row[0];
            Integer qty = ((Number) row[1]).intValue();
            dailyMap.merge(dt.toLocalDate(), qty, Integer::sum);
        }

        SimpleRegression regression = new SimpleRegression(true);
        
        // Feed data into the model (X = days ago, Y = quantity)
        // We use days from start of period as X to make predictions easy
        for (int i = 0; i <= HISTORICAL_DAYS_FOR_FORECAST; i++) {
            LocalDate d = today.minusDays(HISTORICAL_DAYS_FOR_FORECAST - i);
            int qty = dailyMap.getOrDefault(d, 0);
            regression.addData(i, qty);
        }

        // Delete old forecasts for this item
        demandForecastRepository.deleteByMenuItemId(menuItemId);

        List<DemandForecast> forecasts = new ArrayList<>();
        double confidence = Double.isNaN(regression.getRSquare()) ? 0.0 : regression.getRSquare();

        // Predict for future days
        for (int i = 1; i <= forecastDays; i++) {
            LocalDate futureDate = today.plusDays(i);
            int x = HISTORICAL_DAYS_FOR_FORECAST + i;
            
            double predicted = regression.predict(x);
            if (Double.isNaN(predicted) || predicted < 0) {
                predicted = 0.0;
            }

            DemandForecast forecast = DemandForecast.builder()
                    .menuItem(menuItem)
                    .forecastDate(futureDate)
                    .predictedQuantity(predicted)
                    .modelName("SimpleLinearRegression_v1")
                    .confidenceScore(confidence)
                    .build();
            
            forecasts.add(demandForecastRepository.save(forecast));
        }

        log.info("Generated demand forecast for dish {} for next {} days", menuItem.getName(), forecastDays);
        return forecasts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandForecast> getForecast(Long menuItemId) {
        LocalDate today = LocalDate.now();
        return demandForecastRepository.findByMenuItemIdAndForecastDateBetweenOrderByForecastDateAsc(
                menuItemId, today.plusDays(1), today.plusDays(30));
    }

    @Override
    @Transactional
    public void detectSalesAnomalies() {
        List<MenuItem> activeItems = menuItemRepository.findByIsActiveTrue();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        
        LocalDateTime start = today.minusDays(HISTORICAL_DAYS_FOR_ANOMALY + 1).atStartOfDay();
        LocalDateTime end = yesterday.atTime(LocalTime.MAX);

        for (MenuItem item : activeItems) {
            List<Object[]> rawSales = orderItemRepository.findSalesByMenuItemBetween(item.getId(), start, end);
            
            Map<LocalDate, Integer> dailyMap = new TreeMap<>();
            for (Object[] row : rawSales) {
                LocalDateTime dt = (LocalDateTime) row[0];
                Integer qty = ((Number) row[1]).intValue();
                dailyMap.merge(dt.toLocalDate(), qty, Integer::sum);
            }

            DescriptiveStatistics stats = new DescriptiveStatistics();
            for (int i = 1; i <= HISTORICAL_DAYS_FOR_ANOMALY; i++) {
                LocalDate d = yesterday.minusDays(i); // Exclude yesterday from baseline
                stats.addValue(dailyMap.getOrDefault(d, 0));
            }

            double mean = stats.getMean();
            double stdDev = stats.getStandardDeviation();
            int yesterdaySales = dailyMap.getOrDefault(yesterday, 0);

            // Z-Score Anomaly Detection
            if (stdDev > 0) {
                if (yesterdaySales > mean + (2 * stdDev) && yesterdaySales > 5) {
                    createInsight(InsightType.ANOMALY_SPIKE, 
                            "Unusual Spike: " + item.getName(),
                            String.format("Sales for %s surged to %d yesterday (Average is %.1f). Consider increasing stock or running a targeted promotion.", 
                                    item.getName(), yesterdaySales, mean),
                            Severity.INFO);
                } 
                else if (yesterdaySales < mean - (1.5 * stdDev) && mean > 10 && yesterdaySales < (mean * 0.5)) {
                    createInsight(InsightType.ANOMALY_DROP, 
                            "Sales Drop: " + item.getName(),
                            String.format("Sales for %s dropped to %d yesterday (Average is %.1f). Check quality consistency or consider a discount.", 
                                    item.getName(), yesterdaySales, mean),
                            Severity.WARNING);
                }
            }
        }
        log.info("Anomaly detection scan complete.");
    }

    private void createInsight(InsightType type, String title, String description, Severity severity) {
        AIInsight insight = AIInsight.builder()
                .insightType(type)
                .title(title)
                .description(description)
                .severity(severity)
                .build();
        aiInsightRepository.save(insight);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AIInsight> getActiveInsights() {
        return aiInsightRepository.findByIsDismissedFalseOrderByCreatedAtDesc();
    }

    @Override
    @Transactional
    public void dismissInsight(Long insightId) {
        AIInsight insight = aiInsightRepository.findById(insightId)
                .orElseThrow(() -> new ResourceNotFoundException("AIInsight", "id", insightId));
        insight.setIsDismissed(true);
        aiInsightRepository.save(insight);
    }
}
