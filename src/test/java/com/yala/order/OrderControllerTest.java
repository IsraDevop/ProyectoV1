package com.yala.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yala.exception.GlobalExceptionHandler;
import com.yala.exception.InvalidOperationException;
import com.yala.listing.dto.ListingSummaryResponse;
import com.yala.order.controller.OrderController;
import com.yala.order.dto.CreateOrderRequest;
import com.yala.order.dto.OrderResponse;
import com.yala.order.service.OrderService;
import com.yala.security.JwtAuthFilter;
import com.yala.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean OrderService orderService;
    @MockBean JwtAuthFilter jwtAuthFilter;

    private final UserResponse mockUser = new UserResponse(1L, "User", "u@u.com", null, 0f, false, "USER", true);
    private final ListingSummaryResponse mockListing = new ListingSummaryResponse(1L, "Item", "PSA 10", "ACTIVE", 100.0, null, mockUser);
    private final OrderResponse mockOrder = new OrderResponse(1L, 100.0, "PENDING", LocalDateTime.now(),
            mockListing, mockUser, mockUser, 8.0f, 92.0f, LocalDateTime.now().plusHours(48));

    @Test
    @WithMockUser
    void shouldReturn201WhenOrderIsCreated() throws Exception {
        when(orderService.createOrder(any())).thenReturn(mockOrder);

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateOrderRequest(1L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser
    void shouldReturn200WithUserOrders() throws Exception {
        when(orderService.getMyOrders(any())).thenReturn(new PageImpl<>(List.of(mockOrder)));

        mockMvc.perform(get("/api/v1/orders/my-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].amount").value(100.0));
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void shouldReturn409WhenOrderIsAlreadyConfirmed() throws Exception {
        when(orderService.confirmOrder(1L)).thenThrow(new InvalidOperationException("Already confirmed"));

        mockMvc.perform(put("/api/v1/orders/1/confirm"))
                .andExpect(status().isBadRequest());
    }
}
