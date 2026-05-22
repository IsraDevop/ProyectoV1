package com.yala.auth;

import com.yala.auth.dto.LoginRequest;
import com.yala.auth.dto.RegisterRequest;
import com.yala.auth.service.AuthServiceImpl;
import com.yala.exception.DuplicateResourceException;
import com.yala.exception.UnauthorizedException;
import com.yala.security.CciEncryptionService;
import com.yala.security.JwtService;
import com.yala.user.model.Role;
import com.yala.user.model.User;
import com.yala.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock CciEncryptionService cciEncryptionService;

    @InjectMocks AuthServiceImpl authService;

    @Test
    void shouldRegisterUserWhenEmailIsUnique() {
        RegisterRequest req = new RegisterRequest("John", "john@test.com", "Password1");
        when(userRepository.existsByEmail("john@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u = User.builder().id(1L).name(u.getName()).email(u.getEmail())
                    .passwordHash(u.getPasswordHash()).role(u.getRole()).build();
            return u;
        });
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("access");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh");

        var response = authService.register(req);

        assertThat(response.email()).isEqualTo("john@test.com");
        assertThat(response.accessToken()).isEqualTo("access");
    }

    @Test
    void shouldThrowEmailAlreadyExistsExceptionWhenEmailIsDuplicated() {
        RegisterRequest req = new RegisterRequest("John", "john@test.com", "Password1");
        when(userRepository.existsByEmail("john@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    void shouldReturnTokenWhenCredentialsAreValid() {
        LoginRequest req = new LoginRequest("john@test.com", "Password1");
        User user = User.builder().id(1L).name("John").email("john@test.com")
                .passwordHash("hash").role(Role.USER).build();
        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("access");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh");

        var response = authService.login(req);

        assertThat(response.accessToken()).isEqualTo("access");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void shouldThrowUnauthorizedExceptionWhenPasswordIsWrong() {
        LoginRequest req = new LoginRequest("john@test.com", "WrongPass");
        doThrow(new BadCredentialsException("bad")).when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(UnauthorizedException.class);
    }
}
