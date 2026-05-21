package com.yala.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        Long userId,
        String email,
        String name,
        String role
) {}
