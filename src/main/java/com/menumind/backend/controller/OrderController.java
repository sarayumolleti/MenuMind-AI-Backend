package com.menumind.backend.controller;

import com.menumind.backend.dto.common.ApiResponse;
import com.menumind.backend.dto.order.CreateOrderRequest;
import com.menumind.backend.dto.order.OrderResponse;
import com.menumind.backend.dto.order.UpdateOrderStatusRequest;
import com.menumind.backend.entity.OrderStatus;
import com.menumind.backend.entity.OrderType;
import com.menumind.backend.security.UserPrincipal;
import com.menumind.backend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Online Orders REST Controller
 * 
 * Provides endpoints for customers to place orders, track live status,
 * and view digital bills, as well as staff order fulfillment controls.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        OrderResponse orderResponse = orderService.createOnlineOrder(request, currentUser);
        return new ResponseEntity<>(ApiResponse.success("Order placed successfully!", orderResponse), HttpStatus.CREATED);
    }

    @GetMapping("/my-orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<OrderResponse> orders = orderService.getMyOrders(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orders));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        OrderResponse order = orderService.getOrderById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Order details retrieved successfully", order));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_STAFF', 'ROLE_MANAGER', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders(
            @RequestParam(required = false) OrderType type,
            @RequestParam(required = false) OrderStatus status) {
        List<OrderResponse> orders = orderService.getAllOrders(type, status);
        return ResponseEntity.ok(ApiResponse.success("Orders list retrieved successfully", orders));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_STAFF', 'ROLE_MANAGER', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        OrderResponse updatedOrder = orderService.updateOrderStatus(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully", updatedOrder));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        OrderResponse cancelledOrder = orderService.cancelOrder(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", cancelledOrder));
    }
}
