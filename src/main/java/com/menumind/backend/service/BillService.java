package com.menumind.backend.service;

import com.menumind.backend.dto.bill.BillResponse;
import com.menumind.backend.dto.bill.CreateBillRequest;
import com.menumind.backend.security.UserPrincipal;

import java.util.List;

/**
 * Offline / Counter Billing Service Contract
 */
public interface BillService {

    BillResponse createOfflineBill(CreateBillRequest request, UserPrincipal currentStaff);

    BillResponse getBillById(Long billId);

    List<BillResponse> getAllBills();
}
