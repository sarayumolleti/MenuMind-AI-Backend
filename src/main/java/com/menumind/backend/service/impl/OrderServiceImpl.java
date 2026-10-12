package com.menumind.backend.service.impl;

import com.menumind.backend.dto.order.CreateOrderRequest;
import com.menumind.backend.dto.order.OrderItemRequest;
import com.menumind.backend.dto.order.OrderResponse;
import com.menumind.backend.dto.order.UpdateOrderStatusRequest;
import com.menumind.backend.entity.*;
import com.menumind.backend.exception.BadRequestException;
import com.menumind.backend.exception.ResourceNotFoundException;
import com.menumind.backend.repository.MenuItemRepository;
import com.menumind.backend.repository.OrderRepository;
import com.menumind.backend.repository.UserRepository;
import com.menumind.backend.security.UserPrincipal;
import com.menumind.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final BigDecimal GST_RATE = new BigDecimal("0.05"); // 5% Restaurant GST

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public OrderResponse createOnlineOrder(CreateOrderRequest request, UserPrincipal currentUser) {
        User customer = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Order must contain at least one menu item.");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest itemReq : request.getItems()) {
            MenuItem menuItem = menuItemRepository.findById(itemReq.getMenuItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", itemReq.getMenuItemId()));

            if (!menuItem.getIsActive()) {
                throw new BadRequestException("Dish '" + menuItem.getName() + "' is no longer on the active menu.");
            }

            if (!menuItem.getIsAvailable()) {
                throw new BadRequestException("Dish '" + menuItem.getName() + "' is currently out of stock.");
            }

            BigDecimal itemSubtotal = menuItem.getSellingPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            subtotal = subtotal.add(itemSubtotal);

            // Snapshot Accounting: Freeze unitPrice and unitCost at the exact moment of transaction
            OrderItem orderItem = OrderItem.builder()
                    .menuItem(menuItem)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(menuItem.getSellingPrice())
                    .unitCost(menuItem.getEstimatedCost())
                    .subtotal(itemSubtotal)
                    .build();

            orderItems.add(orderItem);
        }

        BigDecimal taxAmount = subtotal.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.add(taxAmount).subtract(discountAmount);

        String orderNumber = "ORD-ONL-" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
                "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .orderType(OrderType.ONLINE)
                .customer(customer)
                .staff(null)
                .orderStatus(OrderStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PAID)
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.ONLINE)
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .build();

        for (OrderItem oi : orderItems) {
            order.addItem(oi);
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Successfully created online order {} for customer {}", savedOrder.getOrderNumber(), customer.getEmail());

        return OrderResponse.fromEntity(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, UserPrincipal currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Security assertion: Customers can only view their own orders
        if (currentUser.getRole() == Role.ROLE_CUSTOMER) {
            if (order.getCustomer() == null || !order.getCustomer().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("You do not have permission to view this order.");
            }
        }

        return OrderResponse.fromEntity(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(UserPrincipal currentUser) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(currentUser.getId()).stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders(OrderType orderType, OrderStatus orderStatus) {
        List<Order> orders;
        if (orderType != null) {
            orders = orderRepository.findByOrderTypeOrderByCreatedAtDesc(orderType);
        } else if (orderStatus != null) {
            orders = orderRepository.findByOrderStatus(orderStatus);
        } else {
            orders = orderRepository.findAll();
        }

        return orders.stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request, UserPrincipal currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        order.setOrderStatus(request.getOrderStatus());
        Order updatedOrder = orderRepository.save(order);
        log.info("Order {} status updated to {} by {}", order.getOrderNumber(), request.getOrderStatus(), currentUser.getEmail());

        return OrderResponse.fromEntity(updatedOrder);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long orderId, UserPrincipal currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Customers can only cancel their own orders
        if (currentUser.getRole() == Role.ROLE_CUSTOMER) {
            if (order.getCustomer() == null || !order.getCustomer().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("You do not have permission to cancel this order.");
            }
            if (order.getOrderStatus() == OrderStatus.PREPARING ||
                order.getOrderStatus() == OrderStatus.READY ||
                order.getOrderStatus() == OrderStatus.COMPLETED) {
                throw new BadRequestException("Order cannot be cancelled as kitchen preparation has already started.");
            }
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        Order updatedOrder = orderRepository.save(order);
        log.info("Order {} cancelled by {}", order.getOrderNumber(), currentUser.getEmail());

        return OrderResponse.fromEntity(updatedOrder);
    }
}
