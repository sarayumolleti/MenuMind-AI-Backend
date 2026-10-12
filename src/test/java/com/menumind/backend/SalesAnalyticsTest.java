package com.menumind.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.menumind.backend.dto.auth.LoginRequest;
import com.menumind.backend.dto.bill.CreateBillRequest;
import com.menumind.backend.dto.order.CreateOrderRequest;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SalesAnalyticsTest
 *
 * Integration tests for Phase 9: Sales & Profit Analytics.
 *
 * Test strategy:
 * 1. First create some orders (online + offline) to seed data
 * 2. Then call analytics endpoints and verify the aggregated results
 * 3. Verify role-based access: Customer cannot access analytics
 */
@SpringBootTest
@AutoConfigureMockMvc
class SalesAnalyticsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MenuItemRepository menuItemRepository;

    private String managerToken;
    private String customerToken;
    private String staffToken;
    private Long testItemId1;
    private Long testItemId2;
    private String today;

    @BeforeEach
    void setUp() throws Exception {
        // Reset menu items to be available
        List<MenuItem> allItems = menuItemRepository.findAll();
        for (MenuItem item : allItems) {
            item.setIsAvailable(true);
            item.setIsActive(true);
        }
        menuItemRepository.saveAll(allItems);

        testItemId1 = allItems.get(0).getId();
        testItemId2 = allItems.get(1).getId();
        today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        // Login as Manager
        managerToken = loginAndGetToken("manager@menumind.com", "Manager@123");
        // Login as Customer
        customerToken = loginAndGetToken("customer@menumind.com", "Customer@123");
        // Login as Staff
        staffToken = loginAndGetToken("staff@menumind.com", "Staff@123");

        // Create an online order (as customer) to have data for analytics
        CreateOrderRequest onlineOrder = CreateOrderRequest.builder()
                .items(List.of(
                        OrderItemRequest.builder().menuItemId(testItemId1).quantity(2).build(),
                        OrderItemRequest.builder().menuItemId(testItemId2).quantity(1).build()
                ))
                .paymentMethod(PaymentMethod.UPI)
                .build();

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(onlineOrder)))
                .andExpect(status().isCreated());

        // Create an offline counter bill (as staff) to have cross-channel data
        CreateBillRequest offlineBill = CreateBillRequest.builder()
                .items(List.of(
                        OrderItemRequest.builder().menuItemId(testItemId1).quantity(3).build()
                ))
                .paymentMethod(PaymentMethod.CASH)
                .tableNumber("Table 5")
                .customerName("Walk-in Customer")
                .build();

        mockMvc.perform(post("/api/bills")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(offlineBill)))
                .andExpect(status().isCreated());
    }

    // -------------------------------------------------------
    // TEST 1: Sales Summary
    // -------------------------------------------------------
    @Test
    void shouldReturnSalesSummary() throws Exception {
        mockMvc.perform(get("/api/analytics/sales-summary")
                        .header("Authorization", "Bearer " + managerToken)
                        .param("startDate", today)
                        .param("endDate", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRevenue").isNumber())
                .andExpect(jsonPath("$.data.totalOrders").isNumber())
                .andExpect(jsonPath("$.data.averageOrderValue").isNumber())
                .andExpect(jsonPath("$.data.onlineOrders").isNumber())
                .andExpect(jsonPath("$.data.offlineOrders").isNumber())
                .andExpect(jsonPath("$.data.onlineRevenue").isNumber())
                .andExpect(jsonPath("$.data.offlineRevenue").isNumber())
                .andExpect(jsonPath("$.data.startDate").value(today))
                .andExpect(jsonPath("$.data.endDate").value(today));
    }

    // -------------------------------------------------------
    // TEST 2: Dish Performance
    // -------------------------------------------------------
    @Test
    void shouldReturnDishPerformance() throws Exception {
        mockMvc.perform(get("/api/analytics/dish-performance")
                        .header("Authorization", "Bearer " + managerToken)
                        .param("startDate", today)
                        .param("endDate", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].menuItemId").isNumber())
                .andExpect(jsonPath("$.data[0].menuItemName").isString())
                .andExpect(jsonPath("$.data[0].totalQuantitySold").isNumber())
                .andExpect(jsonPath("$.data[0].totalRevenue").isNumber())
                .andExpect(jsonPath("$.data[0].totalCost").isNumber())
                .andExpect(jsonPath("$.data[0].totalProfit").isNumber())
                .andExpect(jsonPath("$.data[0].profitMarginPercent").isNumber());
    }

    // -------------------------------------------------------
    // TEST 3: Profit Summary
    // -------------------------------------------------------
    @Test
    void shouldReturnProfitSummary() throws Exception {
        mockMvc.perform(get("/api/analytics/profit-summary")
                        .header("Authorization", "Bearer " + managerToken)
                        .param("startDate", today)
                        .param("endDate", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRevenue").isNumber())
                .andExpect(jsonPath("$.data.totalCOGS").isNumber())
                .andExpect(jsonPath("$.data.grossProfit").isNumber())
                .andExpect(jsonPath("$.data.overallMarginPercent").isNumber())
                .andExpect(jsonPath("$.data.dishBreakdown").isArray())
                .andExpect(jsonPath("$.data.dishBreakdown.length()", greaterThanOrEqualTo(1)));
    }

    // -------------------------------------------------------
    // TEST 4: Category Revenue
    // -------------------------------------------------------
    @Test
    void shouldReturnCategoryRevenue() throws Exception {
        mockMvc.perform(get("/api/analytics/category-revenue")
                        .header("Authorization", "Bearer " + managerToken)
                        .param("startDate", today)
                        .param("endDate", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].categoryId").isNumber())
                .andExpect(jsonPath("$.data[0].categoryName").isString())
                .andExpect(jsonPath("$.data[0].totalItemsSold").isNumber())
                .andExpect(jsonPath("$.data[0].totalRevenue").isNumber())
                .andExpect(jsonPath("$.data[0].totalProfit").isNumber())
                .andExpect(jsonPath("$.data[0].revenueSharePercent").isNumber());
    }

    // -------------------------------------------------------
    // TEST 5: Daily Sales Trend
    // -------------------------------------------------------
    @Test
    void shouldReturnDailySalesTrend() throws Exception {
        mockMvc.perform(get("/api/analytics/daily-trend")
                        .header("Authorization", "Bearer " + managerToken)
                        .param("startDate", today)
                        .param("endDate", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].date").value(today))
                .andExpect(jsonPath("$.data[0].orderCount").isNumber())
                .andExpect(jsonPath("$.data[0].revenue").isNumber());
    }

    // -------------------------------------------------------
    // TEST 6: Defaults to today when no date params
    // -------------------------------------------------------
    @Test
    void shouldDefaultToTodayWhenNoDatesProvided() throws Exception {
        mockMvc.perform(get("/api/analytics/sales-summary")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.startDate").value(today))
                .andExpect(jsonPath("$.data.endDate").value(today));
    }

    // -------------------------------------------------------
    // TEST 7: Customer should be denied access (403)
    // -------------------------------------------------------
    @Test
    void shouldDenyCustomerAccessToAnalytics() throws Exception {
        mockMvc.perform(get("/api/analytics/sales-summary")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/analytics/profit-summary")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------
    // TEST 8: Unauthenticated should get 401
    // -------------------------------------------------------
    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/analytics/sales-summary"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------
    // HELPER: Login and extract JWT token
    // -------------------------------------------------------
    private String loginAndGetToken(String email, String password) throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }
}
