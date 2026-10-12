package com.menumind.backend.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Dish Performance DTO
 *
 * Shows how each menu item performed during a given date range.
 * Includes revenue AND profit metrics so the manager can distinguish
 * "best-selling" dishes from "most-profitable" dishes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DishPerformanceDTO {

    private Long menuItemId;
    private String menuItemName;

    private Long totalQuantitySold;
    private BigDecimal totalRevenue;
    private BigDecimal totalCost;       // COGS: Cost of Goods Sold
    private BigDecimal totalProfit;     // Revenue - COGS
    private BigDecimal profitMarginPercent; // (Profit / Revenue) * 100
}
