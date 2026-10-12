package com.menumind.backend.repository;

import com.menumind.backend.entity.Order;
import com.menumind.backend.entity.OrderStatus;
import com.menumind.backend.entity.OrderType;
import com.menumind.backend.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * OrderRepository
 * 
 * Provides database queries for both online orders and offline counter bills.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Order> findByStaffIdOrderByCreatedAtDesc(Long staffId);

    List<Order> findByOrderTypeOrderByCreatedAtDesc(OrderType orderType);

    List<Order> findByOrderStatus(OrderStatus orderStatus);

    List<Order> findByOrderStatusInOrderByCreatedAtDesc(List<OrderStatus> statuses);

    List<Order> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);

    Long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    Long countByOrderTypeAndCreatedAtBetween(OrderType orderType, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.paymentStatus = 'PAID' AND o.createdAt BETWEEN :start AND :end")
    BigDecimal sumTotalRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderType = :orderType AND o.paymentStatus = 'PAID' AND o.createdAt BETWEEN :start AND :end")
    BigDecimal sumRevenueByOrderTypeBetween(@Param("orderType") OrderType orderType, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Daily revenue + order count time-series for charts.
     * Returns: [year (Integer), month (Integer), day (Integer), orderCount (Long), totalRevenue (BigDecimal)]
     */
    @Query("SELECT YEAR(o.createdAt), MONTH(o.createdAt), DAY(o.createdAt), COUNT(o), COALESCE(SUM(o.totalAmount), 0) " +
           "FROM Order o " +
           "WHERE o.paymentStatus = 'PAID' AND o.createdAt BETWEEN :start AND :end " +
           "GROUP BY YEAR(o.createdAt), MONTH(o.createdAt), DAY(o.createdAt) " +
           "ORDER BY YEAR(o.createdAt) ASC, MONTH(o.createdAt) ASC, DAY(o.createdAt) ASC")
    List<Object[]> findDailyRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Revenue per category aggregation.
     * Returns: [categoryId, categoryName, totalItemsSold, totalRevenue, totalCost]
     */
    @Query("SELECT oi.menuItem.category.id, oi.menuItem.category.name, " +
           "SUM(oi.quantity), SUM(oi.subtotal), SUM(oi.unitCost * oi.quantity) " +
           "FROM OrderItem oi " +
           "WHERE oi.order.paymentStatus = 'PAID' AND oi.order.createdAt BETWEEN :start AND :end " +
           "GROUP BY oi.menuItem.category.id, oi.menuItem.category.name " +
           "ORDER BY SUM(oi.subtotal) DESC")
    List<Object[]> findCategoryRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
