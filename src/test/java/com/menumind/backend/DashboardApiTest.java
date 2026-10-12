package com.menumind.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.menumind.backend.dto.auth.LoginRequest;
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

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MenuItemRepository menuItemRepository;

    private String managerToken;
    private String customerToken;

    @BeforeEach
    void setUp() throws Exception {
        // Prepare menu items
        List<MenuItem> allItems = menuItemRepository.findAll();
        for (MenuItem item : allItems) {
            item.setIsAvailable(true);
            item.setIsActive(true);
        }
        menuItemRepository.saveAll(allItems);

        // Login
        managerToken = loginAndGetToken("manager@menumind.com", "Manager@123");
        customerToken = loginAndGetToken("customer@menumind.com", "Customer@123");

        // Create an order to populate the dashboard
        CreateOrderRequest orderReq = CreateOrderRequest.builder()
                .items(List.of(
                        OrderItemRequest.builder().menuItemId(allItems.get(0).getId()).quantity(1).build()
                ))
                .paymentMethod(PaymentMethod.UPI)
                .build();

        mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderReq)))
            .andExpect(status().isCreated());
    }

    @Test
    void shouldReturnDashboardSummaryForManager() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                .header("Authorization", "Bearer " + managerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.todaySales").exists())
            .andExpect(jsonPath("$.data.todaySales.totalRevenue").isNumber())
            .andExpect(jsonPath("$.data.yesterdaySales").exists())
            .andExpect(jsonPath("$.data.activeOrders").isArray())
            .andExpect(jsonPath("$.data.trendingDishes").isArray());
    }

    @Test
    void shouldDenyDashboardAccessToCustomer() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                .header("Authorization", "Bearer " + customerToken))
            .andExpect(status().isForbidden());
    }

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
