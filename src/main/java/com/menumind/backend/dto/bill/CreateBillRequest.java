package com.menumind.backend.dto.bill;

import com.menumind.backend.dto.order.OrderItemRequest;
import com.menumind.backend.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request payload for Staff Counter Billing (POS)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBillRequest {

    @NotEmpty(message = "Bill must contain at least one menu item")
    @Valid
    private List<OrderItemRequest> items;

    @NotNull(message = "Payment method is required (CASH, CARD, UPI)")
    private PaymentMethod paymentMethod;

    @DecimalMin(value = "0.00", message = "Discount cannot be negative")
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    private String tableNumber;

    private String customerName;

    private String customerPhone;
}
