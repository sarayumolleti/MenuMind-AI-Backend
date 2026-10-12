package com.menumind.backend.controller;

import com.menumind.backend.dto.analytics.*;
import com.menumind.backend.dto.common.ApiResponse;
import com.menumind.backend.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * AnalyticsController
 *
 * REST API endpoints for Sales & Profit Analytics.
 * Access restricted to MANAGER and ADMIN roles only.
 *
 * All endpoints accept optional startDate and endDate query params:
 *   - If not provided → defaults to today
 *   - Format: yyyy-MM-dd  (e.g. 2026-10-09)
 *
 * API Prefix: /api/analytics
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_ADMIN')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // -------------------------------------------------------
    // GET /api/analytics/sales-summary
    // Returns: total revenue, order counts, avg order value,
    //          online/offline breakdown
    // -------------------------------------------------------
    @GetMapping("/sales-summary")
    public ResponseEntity<ApiResponse<SalesSummaryDTO>> getSalesSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDate[] range = resolveRange(startDate, endDate);
        SalesSummaryDTO summary = analyticsService.getSalesSummary(range[0], range[1]);

        return ResponseEntity.ok(ApiResponse.success("Sales summary retrieved", summary));
    }

    // -------------------------------------------------------
    // GET /api/analytics/dish-performance
    // Returns: per-menu-item quantity sold, revenue, COGS,
    //          profit, profit margin
    // -------------------------------------------------------
    @GetMapping("/dish-performance")
    public ResponseEntity<ApiResponse<List<DishPerformanceDTO>>> getDishPerformance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDate[] range = resolveRange(startDate, endDate);
        List<DishPerformanceDTO> performance = analyticsService.getDishPerformance(range[0], range[1]);

        return ResponseEntity.ok(ApiResponse.success("Dish performance retrieved", performance));
    }

    // -------------------------------------------------------
    // GET /api/analytics/profit-summary
    // Returns: total revenue, COGS, gross profit, overall margin,
    //          per-dish breakdown
    // -------------------------------------------------------
    @GetMapping("/profit-summary")
    public ResponseEntity<ApiResponse<ProfitSummaryDTO>> getProfitSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDate[] range = resolveRange(startDate, endDate);
        ProfitSummaryDTO profitSummary = analyticsService.getProfitSummary(range[0], range[1]);

        return ResponseEntity.ok(ApiResponse.success("Profit summary retrieved", profitSummary));
    }

    // -------------------------------------------------------
    // GET /api/analytics/category-revenue
    // Returns: revenue, cost, profit aggregated per category
    // -------------------------------------------------------
    @GetMapping("/category-revenue")
    public ResponseEntity<ApiResponse<List<CategoryRevenueDTO>>> getCategoryRevenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDate[] range = resolveRange(startDate, endDate);
        List<CategoryRevenueDTO> categories = analyticsService.getCategoryRevenue(range[0], range[1]);

        return ResponseEntity.ok(ApiResponse.success("Category revenue retrieved", categories));
    }

    // -------------------------------------------------------
    // GET /api/analytics/daily-trend
    // Returns: time-series of daily revenue + order count
    //          (used by frontend line charts)
    // -------------------------------------------------------
    @GetMapping("/daily-trend")
    public ResponseEntity<ApiResponse<List<DailySalesDTO>>> getDailySalesTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDate[] range = resolveRange(startDate, endDate);
        List<DailySalesDTO> trend = analyticsService.getDailySalesTrend(range[0], range[1]);

        return ResponseEntity.ok(ApiResponse.success("Daily sales trend retrieved", trend));
    }

    // -------------------------------------------------------
    // Helper: Default date range to today if not provided
    // -------------------------------------------------------
    private LocalDate[] resolveRange(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        if (startDate == null) startDate = today;
        if (endDate == null) endDate = today;

        // Swap if user accidentally put start after end
        if (startDate.isAfter(endDate)) {
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }

        return new LocalDate[]{startDate, endDate};
    }
}
