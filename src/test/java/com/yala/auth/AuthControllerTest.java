package com.yala.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yala.auth.controller.AuthController;
import com.yala.auth.dto.AuthResponse;
import com.yala.auth.dto.LoginRequest;
import com.yala.auth.dto.RegisterRequest;
import com.yala.auth.service.AuthService;
import com.yala.exception.DuplicateResourceException;
import com.yala.exception.GlobalExceptionHandler;
import com.yala.exception.UnauthorizedException;
import com.yala.security.JwtAuthFilter;
import com.yala.user.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean AuthService authService;
    @MockBean JwtAuthFilter jwtAuthFilter;

    private final AuthResponse mockAuth = new AuthResponse("access", "refresh", 1L, "test@test.com", "Test", "USER");

    @Test
    void shouldReturn201WhenRegisterIsSuccessful() throws Exception {
        when(authService.register(any())).thenReturn(mockAuth);
        RegisterRequest req = new RegisterRequest("Test User", "test@test.com", "Password1", Role.USER);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access"));
    }

    @Test
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {
        when(authService.register(any())).thenThrow(new DuplicateResourceException("Email already registered"));
        RegisterRequest req = new RegisterRequest("Test User", "test@test.com", "Password1", Role.USER);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn200WithTokenWhenLoginIsSuccessful() throws Exception {
        when(authService.login(any())).thenReturn(mockAuth);
        LoginRequest req = new LoginRequest("test@test.com", "Password1");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"));
    }

    @Test
    void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
        when(authService.login(any())).thenThrow(new UnauthorizedException("Invalid credentials"));
        LoginRequest req = new LoginRequest("wrong@test.com", "WrongPass");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }
}
