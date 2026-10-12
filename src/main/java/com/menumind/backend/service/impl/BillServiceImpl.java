package com.menumind.backend.service.impl;

import com.menumind.backend.dto.bill.BillResponse;
import com.menumind.backend.dto.bill.CreateBillRequest;
import com.menumind.backend.dto.order.OrderItemRequest;
import com.menumind.backend.entity.*;
import com.menumind.backend.exception.BadRequestException;
import com.menumind.backend.exception.ResourceNotFoundException;
import com.menumind.backend.repository.MenuItemRepository;
import com.menumind.backend.repository.OrderRepository;
import com.menumind.backend.repository.UserRepository;
import com.menumind.backend.security.UserPrincipal;
import com.menumind.backend.service.BillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class BillServiceImpl implements BillService {

    private static final BigDecimal GST_RATE = new BigDecimal("0.05"); // 5% Restaurant GST

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public BillResponse createOfflineBill(CreateBillRequest request, UserPrincipal currentStaff) {
        User staff = userRepository.findById(currentStaff.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentStaff.getId()));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Counter bill must contain at least one item.");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> billItems = new ArrayList<>();

        for (OrderItemRequest itemReq : request.getItems()) {
            MenuItem menuItem = menuItemRepository.findById(itemReq.getMenuItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", itemReq.getMenuItemId()));

            if (!menuItem.getIsActive()) {
                throw new BadRequestException("Dish '" + menuItem.getName() + "' is not on the active menu.");
            }

            BigDecimal lineSubtotal = menuItem.getSellingPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            subtotal = subtotal.add(lineSubtotal);

            // Snapshot Accounting: Freeze unitPrice and unitCost at time of counter billing
            OrderItem orderItem = OrderItem.builder()
                    .menuItem(menuItem)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(menuItem.getSellingPrice())
                    .unitCost(menuItem.getEstimatedCost())
                    .subtotal(lineSubtotal)
                    .build();

            billItems.add(orderItem);
        }

        BigDecimal discountAmount = request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO;
        if (discountAmount.compareTo(subtotal) > 0) {
            throw new BadRequestException("Discount cannot exceed total subtotal amount.");
        }

        BigDecimal taxableAmount = subtotal.subtract(discountAmount);
        BigDecimal taxAmount = taxableAmount.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = taxableAmount.add(taxAmount);

        String billNumber = "BILL-POS-" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
                "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Order bill = Order.builder()
                .orderNumber(billNumber)
                .orderType(OrderType.OFFLINE_COUNTER)
                .customer(null) // Walk-in counter customer
                .staff(staff)
                .orderStatus(OrderStatus.COMPLETED)
                .paymentStatus(PaymentStatus.PAID)
                .paymentMethod(request.getPaymentMethod())
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .taxAmount(taxAmount)
                .totalAmount(grandTotal)
                .tableNumber(request.getTableNumber())
                .build();

        for (OrderItem item : billItems) {
            bill.addItem(item);
        }

        Order savedBill = orderRepository.save(bill);
        log.info("Generated counter bill {} for Table {} by Cashier {}",
                savedBill.getOrderNumber(), request.getTableNumber(), staff.getEmail());

        return BillResponse.fromEntity(savedBill);
    }

    @Override
    @Transactional(readOnly = true)
    public BillResponse getBillById(Long billId) {
        Order bill = orderRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill", "id", billId));

        if (bill.getOrderType() != OrderType.OFFLINE_COUNTER) {
            throw new BadRequestException("Requested transaction is an Online Order, not a counter bill.");
        }

        return BillResponse.fromEntity(bill);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillResponse> getAllBills() {
        return orderRepository.findByOrderTypeOrderByCreatedAtDesc(OrderType.OFFLINE_COUNTER).stream()
                .map(BillResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
