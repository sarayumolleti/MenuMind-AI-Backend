package com.menumind.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.menumind.backend.dto.auth.LoginRequest;
import com.menumind.backend.dto.bill.CreateBillRequest;
import com.menumind.backend.dto.order.OrderItemRequest;
import com.menumind.backend.entity.MenuItem;
import com.menumind.backend.entity.PaymentMethod;
import com.menumind.backend.repository.MenuItemRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OfflineBillingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MenuItemRepository menuItemRepository;

    private String staffToken;
    private String customerToken;
    private Long testItemId1;
    private Long testItemId2;

    @BeforeEach
    void setUp() throws Exception {
        List<MenuItem> allItems = menuItemRepository.findAll();
        for (MenuItem item : allItems) {
            item.setIsAvailable(true);
            item.setIsActive(true);
        }
        menuItemRepository.saveAll(allItems);

        testItemId1 = allItems.get(0).getId();
        testItemId2 = allItems.get(1).getId();

        // Authenticate Staff
        LoginRequest staffLogin = LoginRequest.builder()
                .email("staff@menumind.com")
                .password("Staff@123")
                .build();

        MvcResult staffResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staffLogin)))
                .andExpect(status().isOk())
                .andReturn();

        staffToken = JsonPath.read(staffResult.getResponse().getContentAsString(), "$.data.accessToken");

        // Authenticate Customer
        LoginRequest custLogin = LoginRequest.builder()
                .email("customer@menumind.com")
                .password("Customer@123")
                .build();

        MvcResult custResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(custLogin)))
                .andExpect(status().isOk())
                .andReturn();

        customerToken = JsonPath.read(custResult.getResponse().getContentAsString(), "$.data.accessToken");
    }

    @Test
    void shouldCreateCounterBillWithDiscountAndGST() throws Exception {
        CreateBillRequest billRequest = CreateBillRequest.builder()
                .items(List.of(
                        OrderItemRequest.builder().menuItemId(testItemId1).quantity(2).build(),
                        OrderItemRequest.builder().menuItemId(testItemId2).quantity(1).build()
                ))
                .tableNumber("Table 12")
                .discountAmount(new BigDecimal("50.00"))
                .paymentMethod(PaymentMethod.UPI)
                .customerName("Rahul Sharma")
                .customerPhone("9876543210")
                .build();

        MvcResult result = mockMvc.perform(post("/api/bills")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(billRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.invoiceNumber", startsWith("BILL-POS-")))
                .andExpect(jsonPath("$.data.tableNumber").value("Table 12"))
                .andExpect(jsonPath("$.data.paymentMethod").value("UPI"))
                .andExpect(jsonPath("$.data.discountAmount").value(50.00))
                .andExpect(jsonPath("$.data.gstAmount").isNumber())
                .andExpect(jsonPath("$.data.grandTotal").isNumber())
                .andExpect(jsonPath("$.data.cashierStaffName").value("Counter Billing Staff"))
                .andReturn();

        Long billId = ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.data.id")).longValue();

        // Staff can fetch bill details by ID
        mockMvc.perform(get("/api/bills/" + billId)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(billId));
    }

    @Test
    void shouldDenyCustomerFromAccessingCounterBilling() throws Exception {
        CreateBillRequest billRequest = CreateBillRequest.builder()
                .items(List.of(OrderItemRequest.builder().menuItemId(testItemId1).quantity(1).build()))
                .paymentMethod(PaymentMethod.CASH)
                .build();

        mockMvc.perform(post("/api/bills")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(billRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectDiscountExceedingSubtotal() throws Exception {
        CreateBillRequest billRequest = CreateBillRequest.builder()
                .items(List.of(OrderItemRequest.builder().menuItemId(testItemId1).quantity(1).build()))
                .paymentMethod(PaymentMethod.CASH)
                .discountAmount(new BigDecimal("99999.00")) // Exceeds dish cost
                .build();

        mockMvc.perform(post("/api/bills")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(billRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Discount cannot exceed total subtotal amount")));
    }
}
