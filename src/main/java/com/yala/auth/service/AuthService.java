package com.yala.auth.service;

import com.yala.auth.dto.AuthResponse;
import com.yala.auth.dto.LoginRequest;
import com.yala.auth.dto.RefreshTokenRequest;
import com.yala.auth.dto.RegisterRequest;
import com.yala.user.dto.RegisterStoreRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
    AuthResponse registerStore(RegisterStoreRequest request);
}
