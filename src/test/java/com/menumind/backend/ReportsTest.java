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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReportsTest {

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
        List<MenuItem> allItems = menuItemRepository.findAll();
        for (MenuItem item : allItems) {
            item.setIsAvailable(true);
            item.setIsActive(true);
        }
        menuItemRepository.saveAll(allItems);

        managerToken = loginAndGetToken("manager@menumind.com", "Manager@123");
        customerToken = loginAndGetToken("customer@menumind.com", "Customer@123");

        // Create test order to populate report data
        CreateOrderRequest orderReq = CreateOrderRequest.builder()
                .items(List.of(
                        OrderItemRequest.builder().menuItemId(allItems.get(0).getId()).quantity(2).build(),
                        OrderItemRequest.builder().menuItemId(allItems.get(1).getId()).quantity(1).build()
                ))
                .paymentMethod(PaymentMethod.CASH)
                .build();

        mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderReq)))
            .andExpect(status().isCreated());
    }

    @Test
    void shouldReturnBusinessReportForManager() throws Exception {
        mockMvc.perform(get("/api/reports/business")
                .header("Authorization", "Bearer " + managerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.reportTitle").value("MenuMind Business Report"))
            .andExpect(jsonPath("$.data.salesSummary").exists())
            .andExpect(jsonPath("$.data.profitSummary").exists())
            .andExpect(jsonPath("$.data.categoryRevenue").isArray())
            .andExpect(jsonPath("$.data.dailySalesTrend").isArray())
            .andExpect(jsonPath("$.data.topDishes").isArray())
            .andExpect(jsonPath("$.data.menuMatrix").exists());
    }

    @Test
    void shouldExportSalesCsvForManager() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/reports/export/sales")
                .header("Authorization", "Bearer " + managerToken))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", containsString("attachment")))
            .andExpect(header().string("Content-Disposition", containsString(".csv")))
            .andExpect(content().contentType("text/csv"))
            .andReturn();

        String csvContent = result.getResponse().getContentAsString();
        // Verify CSV header row
        assert csvContent.startsWith("Date,Total Revenue");
    }

    @Test
    void shouldExportDishProfitCsvForManager() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/reports/export/dish-profit")
                .header("Authorization", "Bearer " + managerToken))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", containsString(".csv")))
            .andExpect(content().contentType("text/csv"))
            .andReturn();

        String csvContent = result.getResponse().getContentAsString();
        // Verify CSV header row
        assert csvContent.startsWith("Dish Name,Quantity Sold");
    }

    @Test
    void shouldDenyReportAccessToCustomer() throws Exception {
        mockMvc.perform(get("/api/reports/business")
                .header("Authorization", "Bearer " + customerToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldDenyCsvExportToCustomer() throws Exception {
        mockMvc.perform(get("/api/reports/export/sales")
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
