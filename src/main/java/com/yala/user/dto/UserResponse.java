package com.yala.user.dto;

import com.yala.user.model.User;

public record UserResponse(
        Long id,
        String name,
        String email,
        String avatarUrl,
        Float reputation,
        Boolean isVerifiedSeller,
        String role,
        Boolean dniVerified
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getReputation(),
                user.getIsVerifiedSeller(),
                user.getRole().name(),
                user.getDniVerified()
        );
    }
}
