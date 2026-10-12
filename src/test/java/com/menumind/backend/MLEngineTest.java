package com.menumind.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.menumind.backend.dto.auth.LoginRequest;
import com.menumind.backend.entity.MenuItem;
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
class MLEngineTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MenuItemRepository menuItemRepository;

    private String managerToken;
    private String customerToken;
    private Long testMenuItemId;

    @BeforeEach
    void setUp() throws Exception {
        List<MenuItem> allItems = menuItemRepository.findAll();
        for (MenuItem item : allItems) {
            item.setIsAvailable(true);
            item.setIsActive(true);
        }
        menuItemRepository.saveAll(allItems);
        
        testMenuItemId = allItems.get(0).getId();

        managerToken = loginAndGetToken("manager@menumind.com", "Manager@123");
        customerToken = loginAndGetToken("customer@menumind.com", "Customer@123");
    }

    @Test
    void shouldGenerateAndRetrieveDemandForecast() throws Exception {
        // 1. Generate Forecast
        mockMvc.perform(post("/api/ml/forecast/" + testMenuItemId)
                .header("Authorization", "Bearer " + managerToken)
                .param("days", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(5))
            .andExpect(jsonPath("$.data[0].predictedQuantity").isNumber())
            .andExpect(jsonPath("$.data[0].modelName").value("SimpleLinearRegression_v1"));

        // 2. Retrieve Forecast
        mockMvc.perform(get("/api/ml/forecast/" + testMenuItemId)
                .header("Authorization", "Bearer " + managerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(5));
    }

    @Test
    void shouldRunAnomalyScanAndRetrieveInsights() throws Exception {
        // 1. Trigger Scan
        mockMvc.perform(post("/api/ml/insights/scan")
                .header("Authorization", "Bearer " + managerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // 2. Retrieve Insights
        mockMvc.perform(get("/api/ml/insights")
                .header("Authorization", "Bearer " + managerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }
    
    @Test
    void shouldDenyAccessToCustomer() throws Exception {
        mockMvc.perform(post("/api/ml/insights/scan")
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
