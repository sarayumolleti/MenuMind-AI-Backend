package com.menumind.backend.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Sales Summary DTO
 *
 * Provides a complete sales overview for a given date range.
 * Separates revenue by Online vs Offline channels so the manager
 * can see which channel is performing better.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesSummaryDTO {

    // ---------- Overall ----------
    private BigDecimal totalRevenue;
    private Long totalOrders;
    private BigDecimal averageOrderValue;

    // ---------- Channel Breakdown ----------
    private Long onlineOrders;
    private Long offlineOrders;
    private BigDecimal onlineRevenue;
    private BigDecimal offlineRevenue;

    // ---------- Period Info ----------
    private String periodLabel;   // e.g. "Today", "Last 7 Days", "Custom Range"
    private String startDate;
    private String endDate;
}
