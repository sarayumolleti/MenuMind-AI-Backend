package com.menumind.backend.service;

import com.menumind.backend.dto.analytics.*;

import java.time.LocalDate;
import java.util.List;

/**
 * AnalyticsService
 *
 * Defines the contract for all sales and profit analytics operations.
 * Every method accepts a date range so the frontend can request:
 *   - Today, Last 7 Days, Last 30 Days, or a custom range.
 */
public interface AnalyticsService {

    /**
     * Get a complete sales summary: total revenue, order counts, avg order value,
     * online/offline breakdown — all for a given date range.
     */
    SalesSummaryDTO getSalesSummary(LocalDate startDate, LocalDate endDate);

    /**
     * Get per-dish performance: quantity sold, revenue, COGS, profit, margin.
     * Sorted by quantity sold descending (best sellers first).
     */
    List<DishPerformanceDTO> getDishPerformance(LocalDate startDate, LocalDate endDate);

    /**
     * Get aggregated profit report: total revenue vs COGS vs gross profit,
     * plus a per-dish profit breakdown.
     */
    ProfitSummaryDTO getProfitSummary(LocalDate startDate, LocalDate endDate);

    /**
     * Get revenue aggregated by food category.
     */
    List<CategoryRevenueDTO> getCategoryRevenue(LocalDate startDate, LocalDate endDate);

    /**
     * Get daily revenue + order count time-series for chart rendering.
     */
    List<DailySalesDTO> getDailySalesTrend(LocalDate startDate, LocalDate endDate);
}
