package com.menumind.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.menumind.backend.dto.auth.LoginRequest;
import com.menumind.backend.dto.menu.CategoryRequest;
import com.menumind.backend.dto.menu.MenuItemRequest;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MenuManagementTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String managerToken;
    private String customerToken;

    @BeforeEach
    void setUp() throws Exception {
        // Authenticate Manager
        LoginRequest managerLogin = LoginRequest.builder()
                .email("manager@menumind.com")
                .password("Manager@123")
                .build();

        MvcResult mgrResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(managerLogin)))
                .andExpect(status().isOk())
                .andReturn();

        managerToken = JsonPath.read(mgrResult.getResponse().getContentAsString(), "$.data.accessToken");

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
    void shouldAllowPublicAccessToCategories() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void shouldAllowPublicAccessToMenuItems() throws Exception {
        mockMvc.perform(get("/api/menu-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].unitProfit").isNumber())
                .andExpect(jsonPath("$.data[0].profitMarginPercentage").isNumber());
    }

    @Test
    void shouldFilterMenuItemsByKeywordAndVeg() throws Exception {
        mockMvc.perform(get("/api/menu-items?keyword=Biryani&isVeg=false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name", containsString("Biryani")));
    }

    @Test
    void shouldDenyCustomerFromCreatingCategory() throws Exception {
        CategoryRequest catReq = CategoryRequest.builder()
                .name("Unauthorized Category")
                .description("Customer trying to create")
                .build();

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowManagerToCreateMenuItemWithProfitCalculation() throws Exception {
        MenuItemRequest itemReq = MenuItemRequest.builder()
                .categoryId(1L)
                .name("Special Test Kebab " + System.currentTimeMillis())
                .description("Juicy minced kebabs grilled on skewers")
                .sellingPrice(new BigDecimal("300.00"))
                .estimatedCost(new BigDecimal("120.00"))
                .isVeg(false)
                .isAvailable(true)
                .isActive(true)
                .build();

        mockMvc.perform(post("/api/menu-items")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.unitProfit").value(180.00))
                .andExpect(jsonPath("$.data.profitMarginPercentage").value(60.00));
    }

    @Test
    void shouldAllowManagerToToggleItemAvailability() throws Exception {
        mockMvc.perform(patch("/api/menu-items/1/availability?isAvailable=false")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isAvailable").value(false));
    }
}
