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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RecommendationEngineTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MenuItemRepository menuItemRepository;

    private String managerToken;
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

        managerToken = loginAndGetToken("manager@menumind.com", "Manager@123");
        customerToken = loginAndGetToken("customer@menumind.com", "Customer@123");

        // Create an order buying Item 1 and Item 2 together
        CreateOrderRequest orderReq = CreateOrderRequest.builder()
                .items(List.of(
                        OrderItemRequest.builder().menuItemId(testItemId1).quantity(1).build(),
                        OrderItemRequest.builder().menuItemId(testItemId2).quantity(1).build()
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
    void shouldReturnCrossSellRecommendations() throws Exception {
        mockMvc.perform(get("/api/recommendations/cross-sell/" + testItemId1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[*].menuItem.id").value(hasItem(testItemId2.intValue())));
    }

    @Test
    void shouldReturnMenuMatrixForManager() throws Exception {
        mockMvc.perform(get("/api/recommendations/matrix")
                .header("Authorization", "Bearer " + managerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.stars").exists())
            .andExpect(jsonPath("$.data.puzzles").exists())
            .andExpect(jsonPath("$.data.plowhorses").exists())
            .andExpect(jsonPath("$.data.dogs").exists());
    }
    
    @Test
    void shouldDenyMenuMatrixAccessToCustomer() throws Exception {
        mockMvc.perform(get("/api/recommendations/matrix")
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
