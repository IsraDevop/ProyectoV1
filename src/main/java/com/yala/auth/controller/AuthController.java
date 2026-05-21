package com.yala.auth.controller;

import com.yala.auth.dto.AuthResponse;
import com.yala.auth.dto.LoginRequest;
import com.yala.auth.dto.RefreshTokenRequest;
import com.yala.auth.dto.RegisterRequest;
import com.yala.auth.service.AuthService;
import com.yala.user.dto.RegisterStoreRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login and token management")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and get JWT tokens")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/register-store")
    @Operation(summary = "Register a store account")
    public ResponseEntity<AuthResponse> registerStore(@Valid @RequestBody RegisterStoreRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerStore(request));
    }
}
