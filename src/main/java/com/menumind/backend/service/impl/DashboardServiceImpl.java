package com.menumind.backend.service.impl;

import com.menumind.backend.dto.analytics.DishPerformanceDTO;
import com.menumind.backend.dto.analytics.SalesSummaryDTO;
import com.menumind.backend.dto.dashboard.DashboardSummaryDTO;
import com.menumind.backend.dto.order.OrderResponse;
import com.menumind.backend.entity.OrderStatus;
import com.menumind.backend.repository.OrderRepository;
import com.menumind.backend.service.AnalyticsService;
import com.menumind.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final AnalyticsService analyticsService;
    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryDTO getDashboardSummary() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // 1. Get Today's Sales
        SalesSummaryDTO todaySales = analyticsService.getSalesSummary(today, today);

        // 2. Get Yesterday's Sales
        SalesSummaryDTO yesterdaySales = analyticsService.getSalesSummary(yesterday, yesterday);

        // 3. Get Active Orders (PENDING, CONFIRMED, PREPARING, READY)
        List<OrderStatus> activeStatuses = List.of(
                OrderStatus.PENDING,
                OrderStatus.CONFIRMED,
                OrderStatus.PREPARING,
                OrderStatus.READY
        );
        
        List<OrderResponse> activeOrders = orderRepository.findByOrderStatusInOrderByCreatedAtDesc(activeStatuses)
                .stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());

        // 4. Get Trending Dishes Today (limit to top 5)
        List<DishPerformanceDTO> allDishesToday = analyticsService.getDishPerformance(today, today);
        List<DishPerformanceDTO> trendingDishes = allDishesToday.stream()
                .limit(5)
                .collect(Collectors.toList());

        return DashboardSummaryDTO.builder()
                .todaySales(todaySales)
                .yesterdaySales(yesterdaySales)
                .activeOrders(activeOrders)
                .trendingDishes(trendingDishes)
                .build();
    }
}
