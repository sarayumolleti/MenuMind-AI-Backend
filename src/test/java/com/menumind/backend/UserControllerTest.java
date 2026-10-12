package com.menumind.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.menumind.backend.dto.auth.LoginRequest;
import com.menumind.backend.dto.user.CreateUserRequest;
import com.menumind.backend.entity.Role;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

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
    void shouldAllowManagerToFetchUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void shouldDenyCustomerFromFetchingUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowManagerToCreateStaffUser() throws Exception {
        CreateUserRequest staffReq = CreateUserRequest.builder()
                .fullName("New Cashier Staff")
                .email("staff" + System.currentTimeMillis() + "@menumind.com")
                .password("Staff@12345")
                .phone("9876501234")
                .role(Role.ROLE_STAFF)
                .build();

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staffReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("ROLE_STAFF"));
    }

    @Test
    void shouldPreventManagerFromCreatingAdmin() throws Exception {
        CreateUserRequest adminReq = CreateUserRequest.builder()
                .fullName("Unauthorized Admin Attempt")
                .email("fakeadmin" + System.currentTimeMillis() + "@menumind.com")
                .password("Admin@12345")
                .phone("9876501235")
                .role(Role.ROLE_ADMIN)
                .build();

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Restaurant Managers are only permitted to create Staff accounts."));
    }
}
