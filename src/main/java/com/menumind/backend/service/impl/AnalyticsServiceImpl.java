package com.menumind.backend.service.impl;

import com.menumind.backend.dto.analytics.*;
import com.menumind.backend.entity.Order;
import com.menumind.backend.entity.OrderType;
import com.menumind.backend.repository.OrderItemRepository;
import com.menumind.backend.repository.OrderRepository;
import com.menumind.backend.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * AnalyticsServiceImpl
 *
 * Implements all sales and profit analytics logic.
 *
 * KEY DESIGN DECISIONS:
 * 1. Uses snapshot accounting: profits are calculated from order_items.unit_price and
 *    order_items.unit_cost, NOT from menu_items. This means historical profit data
 *    remains accurate even when menu prices change.
 * 2. Only PAID orders are included in all revenue/profit calculations.
 * 3. All BigDecimal computations use scale=2 and HALF_UP rounding.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    // -------------------------------------------------------
    // SALES SUMMARY
    // -------------------------------------------------------
    @Override
    public SalesSummaryDTO getSalesSummary(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        BigDecimal totalRevenue = orderRepository.sumTotalRevenueBetween(start, end);
        Long totalOrders = orderRepository.countByCreatedAtBetween(start, end);

        Long onlineOrders = orderRepository.countByOrderTypeAndCreatedAtBetween(OrderType.ONLINE, start, end);
        Long offlineOrders = orderRepository.countByOrderTypeAndCreatedAtBetween(OrderType.OFFLINE_COUNTER, start, end);

        BigDecimal onlineRevenue = orderRepository.sumRevenueByOrderTypeBetween(OrderType.ONLINE, start, end);
        BigDecimal offlineRevenue = orderRepository.sumRevenueByOrderTypeBetween(OrderType.OFFLINE_COUNTER, start, end);

        BigDecimal avgOrderValue = BigDecimal.ZERO;
        if (totalOrders != null && totalOrders > 0 && totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            avgOrderValue = totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);
        }

        String periodLabel = buildPeriodLabel(startDate, endDate);

        return SalesSummaryDTO.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders != null ? totalOrders : 0L)
                .averageOrderValue(avgOrderValue)
                .onlineOrders(onlineOrders != null ? onlineOrders : 0L)
                .offlineOrders(offlineOrders != null ? offlineOrders : 0L)
                .onlineRevenue(onlineRevenue)
                .offlineRevenue(offlineRevenue)
                .periodLabel(periodLabel)
                .startDate(startDate.format(DATE_FMT))
                .endDate(endDate.format(DATE_FMT))
                .build();
    }

    // -------------------------------------------------------
    // DISH PERFORMANCE
    // -------------------------------------------------------
    @Override
    public List<DishPerformanceDTO> getDishPerformance(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<Object[]> rows = orderItemRepository.findDishPerformanceBetween(start, end);
        List<DishPerformanceDTO> result = new ArrayList<>();

        for (Object[] row : rows) {
            Long menuItemId = ((Number) row[0]).longValue();
            String menuItemName = (String) row[1];
            Long totalQty = ((Number) row[2]).longValue();
            BigDecimal totalRevenue = toBigDecimal(row[3]);
            BigDecimal totalCost = toBigDecimal(row[4]);
            BigDecimal totalProfit = totalRevenue.subtract(totalCost);

            BigDecimal marginPercent = BigDecimal.ZERO;
            if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
                marginPercent = totalProfit
                        .multiply(BigDecimal.valueOf(100))
                        .divide(totalRevenue, 2, RoundingMode.HALF_UP);
            }

            result.add(DishPerformanceDTO.builder()
                    .menuItemId(menuItemId)
                    .menuItemName(menuItemName)
                    .totalQuantitySold(totalQty)
                    .totalRevenue(totalRevenue)
                    .totalCost(totalCost)
                    .totalProfit(totalProfit)
                    .profitMarginPercent(marginPercent)
                    .build());
        }

        return result;
    }

    // -------------------------------------------------------
    // PROFIT SUMMARY
    // -------------------------------------------------------
    @Override
    public ProfitSummaryDTO getProfitSummary(LocalDate startDate, LocalDate endDate) {
        List<DishPerformanceDTO> dishes = getDishPerformance(startDate, endDate);

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalCOGS = BigDecimal.ZERO;

        for (DishPerformanceDTO dish : dishes) {
            totalRevenue = totalRevenue.add(dish.getTotalRevenue());
            totalCOGS = totalCOGS.add(dish.getTotalCost());
        }

        BigDecimal grossProfit = totalRevenue.subtract(totalCOGS);
        BigDecimal overallMargin = BigDecimal.ZERO;
        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            overallMargin = grossProfit
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalRevenue, 2, RoundingMode.HALF_UP);
        }

        String periodLabel = buildPeriodLabel(startDate, endDate);

        return ProfitSummaryDTO.builder()
                .totalRevenue(totalRevenue)
                .totalCOGS(totalCOGS)
                .grossProfit(grossProfit)
                .overallMarginPercent(overallMargin)
                .periodLabel(periodLabel)
                .startDate(startDate.format(DATE_FMT))
                .endDate(endDate.format(DATE_FMT))
                .dishBreakdown(dishes)
                .build();
    }

    // -------------------------------------------------------
    // CATEGORY REVENUE
    // -------------------------------------------------------
    @Override
    public List<CategoryRevenueDTO> getCategoryRevenue(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<Object[]> rows = orderRepository.findCategoryRevenueBetween(start, end);

        // First pass: compute grand total revenue for share percentage
        BigDecimal grandTotalRevenue = BigDecimal.ZERO;
        for (Object[] row : rows) {
            grandTotalRevenue = grandTotalRevenue.add(toBigDecimal(row[3]));
        }

        List<CategoryRevenueDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            Long categoryId = ((Number) row[0]).longValue();
            String categoryName = (String) row[1];
            Long totalItemsSold = ((Number) row[2]).longValue();
            BigDecimal totalRevenue = toBigDecimal(row[3]);
            BigDecimal totalCost = toBigDecimal(row[4]);
            BigDecimal totalProfit = totalRevenue.subtract(totalCost);

            BigDecimal sharePercent = BigDecimal.ZERO;
            if (grandTotalRevenue.compareTo(BigDecimal.ZERO) > 0) {
                sharePercent = totalRevenue
                        .multiply(BigDecimal.valueOf(100))
                        .divide(grandTotalRevenue, 2, RoundingMode.HALF_UP);
            }

            result.add(CategoryRevenueDTO.builder()
                    .categoryId(categoryId)
                    .categoryName(categoryName)
                    .totalItemsSold(totalItemsSold)
                    .totalRevenue(totalRevenue)
                    .totalCost(totalCost)
                    .totalProfit(totalProfit)
                    .revenueSharePercent(sharePercent)
                    .build());
        }

        return result;
    }

    // -------------------------------------------------------
    // DAILY SALES TREND
    // -------------------------------------------------------
    @Override
    public List<DailySalesDTO> getDailySalesTrend(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        // Fetch orders in range and aggregate in Java to avoid JDBC timezone mismatches 
        // with native SQL date functions (e.g. DAY() evaluating the UTC timestamp)
        List<Order> orders = orderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
        
        java.util.Map<LocalDate, DailySalesDTO> dailyMap = new java.util.TreeMap<>();
        
        for (Order o : orders) {
            if (o.getPaymentStatus() != com.menumind.backend.entity.PaymentStatus.PAID) {
                continue;
            }
            LocalDate date = o.getCreatedAt().toLocalDate();
            DailySalesDTO dto = dailyMap.computeIfAbsent(date, d -> 
                DailySalesDTO.builder()
                    .date(d.format(DATE_FMT))
                    .orderCount(0L)
                    .revenue(BigDecimal.ZERO)
                    .build()
            );
            dto.setOrderCount(dto.getOrderCount() + 1);
            dto.setRevenue(dto.getRevenue().add(toBigDecimal(o.getTotalAmount())));
        }

        return new ArrayList<>(dailyMap.values());
    }

    // -------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------

    /**
     * Safely converts an Object (which may be Integer, Long, Double, or BigDecimal)
     * to BigDecimal with scale=2.
     */
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return ((BigDecimal) value).setScale(2, RoundingMode.HALF_UP);
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue()).setScale(2, RoundingMode.HALF_UP);
        return BigDecimal.ZERO;
    }

    /**
     * Builds a human-readable period label.
     */
    private String buildPeriodLabel(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        if (startDate.equals(today) && endDate.equals(today)) {
            return "Today";
        } else if (startDate.equals(today.minusDays(6)) && endDate.equals(today)) {
            return "Last 7 Days";
        } else if (startDate.equals(today.minusDays(29)) && endDate.equals(today)) {
            return "Last 30 Days";
        } else {
            return startDate.format(DATE_FMT) + " to " + endDate.format(DATE_FMT);
        }
    }
}
