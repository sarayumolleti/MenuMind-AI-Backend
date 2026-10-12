package com.menumind.backend.repository;

import com.menumind.backend.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OrderItemRepository
 * 
 * Provides database queries for line items, dish-level profit aggregation,
 * and time-series extraction for the Java ML engine.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    /**
     * Aggregates total quantity sold and revenue generated per menu item.
     * Returns an Object array: [menuItemId, menuItemName, totalQuantitySold, totalRevenue, totalCost]
     */
    @Query("SELECT oi.menuItem.id, oi.menuItem.name, SUM(oi.quantity), SUM(oi.subtotal), SUM(oi.unitCost * oi.quantity) " +
           "FROM OrderItem oi " +
           "WHERE oi.order.paymentStatus = 'PAID' AND oi.order.createdAt BETWEEN :start AND :end " +
           "GROUP BY oi.menuItem.id, oi.menuItem.name " +
           "ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> findDishPerformanceBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Extracts sales for a specific menu item over a date range.
     * Returns raw timestamps and quantities to be aggregated by date in Java,
     * avoiding JDBC timezone truncation bugs.
     * Returns: [LocalDateTime, Integer]
     */
    @Query("SELECT oi.order.createdAt, oi.quantity " +
           "FROM OrderItem oi " +
           "WHERE oi.menuItem.id = :menuItemId AND oi.order.paymentStatus = 'PAID' AND oi.order.createdAt BETWEEN :start AND :end " +
           "ORDER BY oi.order.createdAt ASC")
    List<Object[]> findSalesByMenuItemBetween(@Param("menuItemId") Long menuItemId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Collaborative Filtering: Finds items most frequently bought in the same order as a given item.
     * Returns: [menuItemId, count]
     */
    @Query("SELECT oi2.menuItem.id, COUNT(oi2.id) " +
           "FROM OrderItem oi1 " +
           "JOIN OrderItem oi2 ON oi1.order.id = oi2.order.id " +
           "WHERE oi1.menuItem.id = :menuItemId AND oi2.menuItem.id != :menuItemId AND oi1.order.paymentStatus = 'PAID' " +
           "GROUP BY oi2.menuItem.id " +
           "ORDER BY COUNT(oi2.id) DESC")
    List<Object[]> findFrequentlyBoughtTogether(@Param("menuItemId") Long menuItemId);
}
