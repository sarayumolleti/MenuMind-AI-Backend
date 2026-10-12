package com.menumind.backend.dto.report;

import com.menumind.backend.dto.analytics.*;
import com.menumind.backend.dto.recommendation.MenuEngineeringMatrixDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Business Report DTO
 *
 * A comprehensive business report combining sales, profit, category revenue,
 * daily trends, and menu engineering insights into a single exportable payload.
 * Designed for manager/admin review and stakeholder presentations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessReportDTO {

    // ---------- Metadata ----------
    private String reportTitle;
    private String periodLabel;
    private String startDate;
    private String endDate;
    private LocalDateTime generatedAt;

    // ---------- Financial Summary ----------
    private SalesSummaryDTO salesSummary;
    private ProfitSummaryDTO profitSummary;

    // ---------- Category Breakdown ----------
    private List<CategoryRevenueDTO> categoryRevenue;

    // ---------- Daily Trends ----------
    private List<DailySalesDTO> dailySalesTrend;

    // ---------- Dish Rankings ----------
    private List<DishPerformanceDTO> topDishes;        // Top 10 by volume
    private List<DishPerformanceDTO> bottomDishes;     // Bottom 5 by volume

    // ---------- Menu Engineering ----------
    private MenuEngineeringMatrixDTO menuMatrix;
}
