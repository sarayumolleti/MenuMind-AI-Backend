package com.menumind.backend.dto.bill;

import com.menumind.backend.dto.order.OrderItemResponse;
import com.menumind.backend.entity.Order;
import com.menumind.backend.entity.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Digital Counter Bill / Tax Invoice Schema
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillResponse {

    private Long id;
    private String invoiceNumber;

    // Restaurant details
    @Builder.Default
    private String restaurantName = "MenuMind Gourmet Restaurant";
    @Builder.Default
    private String gstin = "36ABCDE1234F1Z5";
    @Builder.Default
    private String restaurantAddress = "Plot 42, Knowledge Park, Hitech City, Hyderabad, 500081";

    // Bill metadata
    private String tableNumber;
    private Long cashierStaffId;
    private String cashierStaffName;
    private String customerName;
    private String customerPhone;
    private PaymentMethod paymentMethod;

    // Financial breakdown
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxableAmount;
    private BigDecimal gstAmount; // 5% GST
    private BigDecimal grandTotal;

    private List<OrderItemResponse> items;
    private LocalDateTime billedAt;

    public static BillResponse fromEntity(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems() != null ?
                order.getItems().stream()
                        .map(OrderItemResponse::fromEntity)
                        .collect(Collectors.toList()) :
                Collections.emptyList();

        BigDecimal discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal taxable = order.getSubtotal().subtract(discount).max(BigDecimal.ZERO);

        return BillResponse.builder()
                .id(order.getId())
                .invoiceNumber(order.getOrderNumber())
                .tableNumber(order.getTableNumber())
                .cashierStaffId(order.getStaff() != null ? order.getStaff().getId() : null)
                .cashierStaffName(order.getStaff() != null ? order.getStaff().getFullName() : "Counter Cashier")
                .customerName(order.getCustomer() != null ? order.getCustomer().getFullName() : "Walk-in Guest")
                .customerPhone(order.getCustomer() != null ? order.getCustomer().getPhone() : null)
                .paymentMethod(order.getPaymentMethod())
                .subtotal(order.getSubtotal())
                .discountAmount(discount)
                .taxableAmount(taxable)
                .gstAmount(order.getTaxAmount())
                .grandTotal(order.getTotalAmount())
                .items(itemResponses)
                .billedAt(order.getCreatedAt())
                .build();
    }
}
