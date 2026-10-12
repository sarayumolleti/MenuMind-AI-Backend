package com.menumind.backend.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Profit Summary DTO
 *
 * Aggregated profit report for a given date range.
 * REVENUE ≠ PROFIT. This DTO makes the distinction clear:
 *   Gross Profit = Total Revenue - Total COGS (Cost of Goods Sold)
 *   Overall Margin = (Gross Profit / Total Revenue) * 100
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfitSummaryDTO {

    private BigDecimal totalRevenue;
    private BigDecimal totalCOGS;           // Cost of Goods Sold
    private BigDecimal grossProfit;          // Revenue - COGS
    private BigDecimal overallMarginPercent; // (grossProfit / totalRevenue) * 100

    // ---------- Period Info ----------
    private String periodLabel;
    private String startDate;
    private String endDate;

    // ---------- Per-Dish Breakdown ----------
    private List<DishPerformanceDTO> dishBreakdown;
}
