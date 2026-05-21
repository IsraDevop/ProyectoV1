package com.yala.user.service;

import com.yala.user.dto.UpdateUserRequest;
import com.yala.user.dto.UserResponse;
import com.yala.user.dto.VerifyIdentityRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserResponse getMe();
    UserResponse updateMe(UpdateUserRequest request);
    UserResponse getUserById(Long id);
    UserResponse verifyIdentity(VerifyIdentityRequest request);
    void requestSellerVerification();
}
