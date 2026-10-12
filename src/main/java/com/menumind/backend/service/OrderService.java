package com.menumind.backend.service;

import com.menumind.backend.dto.order.CreateOrderRequest;
import com.menumind.backend.dto.order.OrderResponse;
import com.menumind.backend.dto.order.UpdateOrderStatusRequest;
import com.menumind.backend.entity.OrderStatus;
import com.menumind.backend.entity.OrderType;
import com.menumind.backend.security.UserPrincipal;

import java.util.List;

/**
 * Online Ordering Service Contract
 */
public interface OrderService {

    OrderResponse createOnlineOrder(CreateOrderRequest request, UserPrincipal currentUser);

    OrderResponse getOrderById(Long orderId, UserPrincipal currentUser);

    List<OrderResponse> getMyOrders(UserPrincipal currentUser);

    List<OrderResponse> getAllOrders(OrderType orderType, OrderStatus orderStatus);

    OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request, UserPrincipal currentUser);

    OrderResponse cancelOrder(Long orderId, UserPrincipal currentUser);
}
