package com.menumind.backend.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Category Revenue DTO
 *
 * Revenue aggregated at the category level.
 * Helps managers see which food categories (Starters, Main Course, Desserts, etc.)
 * generate the most revenue and profit.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRevenueDTO {

    private Long categoryId;
    private String categoryName;

    private Long totalItemsSold;
    private BigDecimal totalRevenue;
    private BigDecimal totalCost;
    private BigDecimal totalProfit;
    private BigDecimal revenueSharePercent; // This category's share of total revenue
}
