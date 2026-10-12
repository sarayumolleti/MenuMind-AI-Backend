package com.menumind.backend.service.impl;

import com.menumind.backend.dto.analytics.DishPerformanceDTO;
import com.menumind.backend.dto.menu.MenuItemResponse;
import com.menumind.backend.dto.recommendation.MenuEngineeringMatrixDTO;
import com.menumind.backend.dto.recommendation.MenuEngineeringMatrixDTO.MatrixItemDTO;
import com.menumind.backend.dto.recommendation.RecommendationItemDTO;
import com.menumind.backend.entity.MenuItem;
import com.menumind.backend.repository.MenuItemRepository;
import com.menumind.backend.repository.OrderItemRepository;
import com.menumind.backend.service.AnalyticsService;
import com.menumind.backend.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final AnalyticsService analyticsService;

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationItemDTO> getCrossSellRecommendations(Long menuItemId) {
        List<Object[]> frequentlyBought = orderItemRepository.findFrequentlyBoughtTogether(menuItemId);
        
        List<RecommendationItemDTO> recommendations = new ArrayList<>();
        int count = 0;
        
        for (Object[] row : frequentlyBought) {
            if (count >= 5) break; // Limit to Top 5
            
            Long id = ((Number) row[0]).longValue();
            Long coOccurrenceScore = ((Number) row[1]).longValue();
            
            menuItemRepository.findById(id).ifPresent(item -> {
                if (item.getIsActive() && item.getIsAvailable()) {
                    recommendations.add(RecommendationItemDTO.builder()
                            .menuItem(MenuItemResponse.fromEntity(item))
                            .recommendationReason("Frequently bought together")
                            .matchScore(coOccurrenceScore.doubleValue())
                            .build());
                }
            });
            count++;
        }
        
        return recommendations;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationItemDTO> getPersonalizedRecommendations(Long customerId) {
        // Simplified personalization: just get their past orders and recommend active items they bought often
        // In a real system, we'd do category affinity.
        LocalDate past30Days = LocalDate.now().minusDays(30);
        
        // This is a naive recommendation: just returns popular items today as a fallback
        // Since we don't have a direct query for customer's favorite items in the repository,
        // we will just recommend the top 3 items globally and call it "Trending now".
        List<DishPerformanceDTO> trending = analyticsService.getDishPerformance(past30Days, LocalDate.now());
        
        List<RecommendationItemDTO> recommendations = new ArrayList<>();
        int count = 0;
        for (DishPerformanceDTO dish : trending) {
            if (count >= 3) break;
            menuItemRepository.findById(dish.getMenuItemId()).ifPresent(item -> {
                if (item.getIsActive() && item.getIsAvailable()) {
                    recommendations.add(RecommendationItemDTO.builder()
                            .menuItem(MenuItemResponse.fromEntity(item))
                            .recommendationReason("Trending Right Now")
                            .matchScore(dish.getTotalQuantitySold().doubleValue())
                            .build());
                }
            });
            count++;
        }
        
        return recommendations;
    }

    @Override
    @Transactional(readOnly = true)
    public MenuEngineeringMatrixDTO getMenuEngineeringMatrix() {
        // Analyze past 30 days
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(30);
        
        List<DishPerformanceDTO> performance = analyticsService.getDishPerformance(start, end);
        
        if (performance.isEmpty()) {
            return new MenuEngineeringMatrixDTO(List.of(), List.of(), List.of(), List.of());
        }

        // Calculate Average Volume and Average Profit Margin to establish the matrix axes
        DescriptiveStatistics volumeStats = new DescriptiveStatistics();
        DescriptiveStatistics profitStats = new DescriptiveStatistics();
        
        for (DishPerformanceDTO dto : performance) {
            volumeStats.addValue(dto.getTotalQuantitySold());
            if (dto.getTotalQuantitySold() > 0) {
                // Average unit profit = total profit / total quantity
                double unitProfit = dto.getTotalProfit().doubleValue() / dto.getTotalQuantitySold();
                profitStats.addValue(unitProfit);
            }
        }
        
        double medianVolume = volumeStats.getPercentile(50);
        double medianUnitProfit = profitStats.getPercentile(50);

        List<MatrixItemDTO> stars = new ArrayList<>();
        List<MatrixItemDTO> puzzles = new ArrayList<>();
        List<MatrixItemDTO> plowhorses = new ArrayList<>();
        List<MatrixItemDTO> dogs = new ArrayList<>();
        
        for (DishPerformanceDTO dto : performance) {
            double volume = dto.getTotalQuantitySold();
            double unitProfit = volume > 0 ? (dto.getTotalProfit().doubleValue() / volume) : 0;
            
            MatrixItemDTO matrixItem = MatrixItemDTO.builder()
                    .menuItemId(dto.getMenuItemId())
                    .name(dto.getMenuItemName())
                    .totalVolume(dto.getTotalQuantitySold())
                    .totalProfit(dto.getTotalProfit())
                    .unitProfit(java.math.BigDecimal.valueOf(unitProfit).setScale(2, java.math.RoundingMode.HALF_UP))
                    .build();
            
            boolean highVolume = volume >= medianVolume;
            boolean highProfit = unitProfit >= medianUnitProfit;
            
            if (highVolume && highProfit) {
                stars.add(matrixItem);
            } else if (!highVolume && highProfit) {
                puzzles.add(matrixItem);
            } else if (highVolume && !highProfit) {
                plowhorses.add(matrixItem);
            } else {
                dogs.add(matrixItem);
            }
        }

        return MenuEngineeringMatrixDTO.builder()
                .stars(stars)
                .puzzles(puzzles)
                .plowhorses(plowhorses)
                .dogs(dogs)
                .build();
    }
}
