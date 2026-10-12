package com.menumind.backend.dto.dashboard;

import com.menumind.backend.dto.analytics.DishPerformanceDTO;
import com.menumind.backend.dto.analytics.SalesSummaryDTO;
import com.menumind.backend.dto.order.OrderResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Dashboard Summary DTO
 *
 * An aggregated snapshot designed for the restaurant manager's main screen.
 * It combines today's sales, active ongoing orders, and top trending dishes
 * into a single API payload, preventing the frontend from making multiple heavy calls.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDTO {

    // Snapshot of today's revenue & order count
    private SalesSummaryDTO todaySales;

    // Snapshot of yesterday's revenue & order count (for % growth comparison)
    private SalesSummaryDTO yesterdaySales;

    // Orders currently in progress (PENDING, PREPARING)
    private List<OrderResponse> activeOrders;

    // The most popular dishes sold today (limited to top 5)
    private List<DishPerformanceDTO> trendingDishes;
}
