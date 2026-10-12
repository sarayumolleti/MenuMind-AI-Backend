package com.menumind.backend.service.impl;

import com.menumind.backend.dto.analytics.*;
import com.menumind.backend.dto.recommendation.MenuEngineeringMatrixDTO;
import com.menumind.backend.dto.report.BusinessReportDTO;
import com.menumind.backend.service.AnalyticsService;
import com.menumind.backend.service.RecommendationService;
import com.menumind.backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final AnalyticsService analyticsService;
    private final RecommendationService recommendationService;

    @Override
    @Transactional(readOnly = true)
    public BusinessReportDTO generateBusinessReport(LocalDate startDate, LocalDate endDate) {

        // Aggregate all analytics
        SalesSummaryDTO sales = analyticsService.getSalesSummary(startDate, endDate);
        ProfitSummaryDTO profit = analyticsService.getProfitSummary(startDate, endDate);
        List<CategoryRevenueDTO> categoryRevenue = analyticsService.getCategoryRevenue(startDate, endDate);
        List<DailySalesDTO> dailyTrend = analyticsService.getDailySalesTrend(startDate, endDate);
        List<DishPerformanceDTO> allDishes = analyticsService.getDishPerformance(startDate, endDate);

        // Top 10 dishes by volume
        List<DishPerformanceDTO> topDishes = allDishes.stream()
                .limit(10)
                .collect(Collectors.toList());

        // Bottom 5 dishes by volume (reverse order)
        List<DishPerformanceDTO> bottomDishes = allDishes.stream()
                .skip(Math.max(0, allDishes.size() - 5))
                .collect(Collectors.toList());

        // Menu Engineering Matrix
        MenuEngineeringMatrixDTO menuMatrix = recommendationService.getMenuEngineeringMatrix();

        // Build period label
        String periodLabel;
        if (startDate.equals(endDate)) {
            periodLabel = "Daily Report — " + startDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        } else {
            periodLabel = startDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) +
                    " to " + endDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        }

        log.info("Generated business report for period: {}", periodLabel);

        return BusinessReportDTO.builder()
                .reportTitle("MenuMind Business Report")
                .periodLabel(periodLabel)
                .startDate(startDate.toString())
                .endDate(endDate.toString())
                .generatedAt(LocalDateTime.now())
                .salesSummary(sales)
                .profitSummary(profit)
                .categoryRevenue(categoryRevenue)
                .dailySalesTrend(dailyTrend)
                .topDishes(topDishes)
                .bottomDishes(bottomDishes)
                .menuMatrix(menuMatrix)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public String exportSalesReportCsv(LocalDate startDate, LocalDate endDate) {
        List<DailySalesDTO> dailyTrend = analyticsService.getDailySalesTrend(startDate, endDate);

        StringBuilder csv = new StringBuilder();
        csv.append("Date,Total Revenue (₹),Order Count\n");

        for (DailySalesDTO day : dailyTrend) {
            csv.append(day.getDate()).append(",");
            csv.append(day.getRevenue()).append(",");
            csv.append(day.getOrderCount()).append("\n");
        }

        log.info("Exported sales CSV report ({} to {})", startDate, endDate);
        return csv.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public String exportDishProfitCsv(LocalDate startDate, LocalDate endDate) {
        List<DishPerformanceDTO> dishes = analyticsService.getDishPerformance(startDate, endDate);

        StringBuilder csv = new StringBuilder();
        csv.append("Dish Name,Quantity Sold,Revenue (₹),COGS (₹),Profit (₹),Margin (%)\n");

        for (DishPerformanceDTO dish : dishes) {
            csv.append(escapeCsv(dish.getMenuItemName())).append(",");
            csv.append(dish.getTotalQuantitySold()).append(",");
            csv.append(dish.getTotalRevenue()).append(",");
            csv.append(dish.getTotalCost()).append(",");
            csv.append(dish.getTotalProfit()).append(",");
            csv.append(dish.getProfitMarginPercent()).append("\n");
        }

        log.info("Exported dish profit CSV report ({} to {})", startDate, endDate);
        return csv.toString();
    }

    /**
     * Escapes a CSV field value: wraps in quotes if it contains commas or quotes.
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
