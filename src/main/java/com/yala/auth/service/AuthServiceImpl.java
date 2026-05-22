package com.yala.auth.service;

import com.yala.auth.dto.AuthResponse;
import com.yala.auth.dto.LoginRequest;
import com.yala.auth.dto.RefreshTokenRequest;
import com.yala.auth.dto.RegisterRequest;
import com.yala.event.UserRegisteredEvent;
import com.yala.exception.DuplicateResourceException;
import com.yala.exception.UnauthorizedException;
import com.yala.security.CciEncryptionService;
import com.yala.security.JwtService;
import com.yala.user.dto.RegisterStoreRequest;
import com.yala.user.model.Role;
import com.yala.user.model.User;
import com.yala.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final ApplicationEventPublisher eventPublisher;
    private final CciEncryptionService cciEncryptionService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }
        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();
        userRepository.save(user);
        eventPublisher.publishEvent(new UserRegisteredEvent(this, user.getId(), user.getEmail(), user.getName()));
        return buildAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        return buildAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String token = request.refreshToken();
        if (!jwtService.isValid(token)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }
        String email = jwtService.extractEmail(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse registerStore(RegisterStoreRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }
        User store = User.builder()
                .name(request.storeName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .cci(cciEncryptionService.encrypt(request.cci()))
                .role(Role.USER)
                .isVerifiedSeller(false)
                .build();
        userRepository.save(store);
        eventPublisher.publishEvent(new UserRegisteredEvent(this, store.getId(), store.getEmail(), store.getName()));
        return buildAuthResponse(store);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());
        return new AuthResponse(accessToken, refreshToken, user.getId(), user.getEmail(), user.getName(), user.getRole().name());
    }
}
