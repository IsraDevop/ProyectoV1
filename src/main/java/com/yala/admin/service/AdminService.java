package com.yala.admin.service;

import com.yala.user.dto.UserResponse;

public interface AdminService {
    UserResponse approveStore(Long userId);
    UserResponse approveSellerVerification(Long userId);
}
