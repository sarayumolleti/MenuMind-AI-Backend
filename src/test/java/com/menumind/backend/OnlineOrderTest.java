package com.menumind.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.menumind.backend.dto.auth.LoginRequest;
import com.menumind.backend.dto.auth.RegisterRequest;
import com.menumind.backend.dto.order.CreateOrderRequest;
import com.menumind.backend.dto.order.OrderItemRequest;
import com.menumind.backend.dto.order.UpdateOrderStatusRequest;
import com.menumind.backend.entity.MenuItem;
import com.menumind.backend.entity.OrderStatus;
import com.menumind.backend.entity.Role;
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
class OnlineOrderTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MenuItemRepository menuItemRepository;

    private String customerToken;
    private String staffToken;
    private Long availableItem1Id;
    private Long availableItem2Id;

    @BeforeEach
    void setUp() throws Exception {
        // Ensure menu items are active and available for order testing
        List<MenuItem> allItems = menuItemRepository.findAll();
        for (MenuItem item : allItems) {
            item.setIsAvailable(true);
            item.setIsActive(true);
        }
        menuItemRepository.saveAll(allItems);

        availableItem1Id = allItems.get(0).getId();
        availableItem2Id = allItems.get(1).getId();

        // Authenticate Customer
        LoginRequest customerLogin = LoginRequest.builder()
                .email("customer@menumind.com")
                .password("Customer@123")
                .build();

        MvcResult custResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerLogin)))
                .andExpect(status().isOk())
                .andReturn();

        customerToken = JsonPath.read(custResult.getResponse().getContentAsString(), "$.data.accessToken");

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
    }

    @Test
    void shouldCreateOnlineOrderWithTaxAndSnapshotAccounting() throws Exception {
        CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                .items(List.of(
                        OrderItemRequest.builder().menuItemId(availableItem1Id).quantity(2).build(),
                        OrderItemRequest.builder().menuItemId(availableItem2Id).quantity(1).build()
                ))
                .deliveryAddress("Flat 402, Green Acres Apt, Hyderabad")
                .build();

        MvcResult result = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderType").value("ONLINE"))
                .andExpect(jsonPath("$.data.subtotal").isNumber())
                .andExpect(jsonPath("$.data.taxAmount").isNumber())
                .andExpect(jsonPath("$.data.totalAmount").isNumber())
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.items[0].unitPrice").isNumber())
                .andExpect(jsonPath("$.data.items[0].unitCost").isNumber())
                .andReturn();

        Long orderId = ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.data.id")).longValue();

        // Customer can fetch digital bill by ID
        mockMvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(orderId))
                .andExpect(jsonPath("$.data.customerEmail").value("customer@menumind.com"));

        // Staff can update order status to PREPARING
        UpdateOrderStatusRequest updateReq = UpdateOrderStatusRequest.builder()
                .orderStatus(OrderStatus.PREPARING)
                .build();

        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("PREPARING"));
    }

    @Test
    void shouldRejectOrderWhenDishIsOutOfStock() throws Exception {
        MenuItem item = menuItemRepository.findById(availableItem1Id).orElseThrow();
        item.setIsAvailable(false);
        menuItemRepository.save(item);

        CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                .items(List.of(OrderItemRequest.builder().menuItemId(availableItem1Id).quantity(1).build()))
                .build();

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("is currently out of stock")));

        // Restore availability
        item.setIsAvailable(true);
        menuItemRepository.save(item);
    }

    @Test
    void shouldFetchCustomerPersonalOrderHistory() throws Exception {
        mockMvc.perform(get("/api/orders/my-orders")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void shouldDenyCustomerFromAccessingAnotherCustomersOrder() throws Exception {
        // Register a second customer
        RegisterRequest otherCust = RegisterRequest.builder()
                .fullName("Other Customer")
                .email("other" + System.currentTimeMillis() + "@gmail.com")
                .password("Pass@123")
                .role(Role.ROLE_CUSTOMER)
                .build();

        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherCust)))
                .andExpect(status().isCreated())
                .andReturn();

        String otherCustomerToken = JsonPath.read(regResult.getResponse().getContentAsString(), "$.data.accessToken");

        // Customer 1 places an order
        CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                .items(List.of(OrderItemRequest.builder().menuItemId(availableItem2Id).quantity(1).build()))
                .build();

        MvcResult orderResult = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long orderId = ((Number) JsonPath.read(orderResult.getResponse().getContentAsString(), "$.data.id")).longValue();

        // Customer 2 attempts to view Customer 1's order -> Expected: 403 Forbidden
        mockMvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + otherCustomerToken))
                .andExpect(status().isForbidden());
    }
}
