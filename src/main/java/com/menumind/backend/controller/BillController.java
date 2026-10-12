package com.menumind.backend.controller;

import com.menumind.backend.dto.bill.BillResponse;
import com.menumind.backend.dto.bill.CreateBillRequest;
import com.menumind.backend.dto.common.ApiResponse;
import com.menumind.backend.security.UserPrincipal;
import com.menumind.backend.service.BillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Offline / Counter Billing REST Controller (POS)
 * 
 * Provides fast billing endpoints for restaurant staff and managers:
 * generates tax invoices, applies counter discounts, records table numbers,
 * and accepts Cash/UPI/Card payments.
 */
@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_STAFF', 'ROLE_MANAGER', 'ROLE_ADMIN')")
public class BillController {

    private final BillService billService;

    @PostMapping
    public ResponseEntity<ApiResponse<BillResponse>> createBill(
            @Valid @RequestBody CreateBillRequest request,
            @AuthenticationPrincipal UserPrincipal currentStaff) {
        BillResponse billResponse = billService.createOfflineBill(request, currentStaff);
        return new ResponseEntity<>(ApiResponse.success("Counter bill generated successfully!", billResponse), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BillResponse>> getBillById(@PathVariable Long id) {
        BillResponse bill = billService.getBillById(id);
        return ResponseEntity.ok(ApiResponse.success("Bill details retrieved successfully", bill));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BillResponse>>> getAllBills() {
        List<BillResponse> bills = billService.getAllBills();
        return ResponseEntity.ok(ApiResponse.success("Counter bills retrieved successfully", bills));
    }
}
