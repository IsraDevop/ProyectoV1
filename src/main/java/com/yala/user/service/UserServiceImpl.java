package com.yala.user.service;

import com.yala.exception.DniAlreadyExistsException;
import com.yala.exception.InvalidOperationException;
import com.yala.exception.ResourceNotFoundException;
import com.yala.user.dto.UpdateUserRequest;
import com.yala.user.dto.UserResponse;
import com.yala.user.dto.VerifyIdentityRequest;
import com.yala.user.model.User;
import com.yala.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMe() {
        return UserResponse.from(getCurrentUser());
    }

    @Override
    @Transactional
    public UserResponse updateMe(UpdateUserRequest request) {
        User user = getCurrentUser();
        if (request.name() != null) user.setName(request.name());
        if (request.avatarUrl() != null) user.setAvatarUrl(request.avatarUrl());
        return UserResponse.from(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        return UserResponse.from(user);
    }

    @Override
    @Transactional
    public UserResponse verifyIdentity(VerifyIdentityRequest request) {
        User user = getCurrentUser();
        if (userRepository.existsByDni(request.dni()) && !request.dni().equals(user.getDni())) {
            throw new DniAlreadyExistsException("Este DNI ya está asociado a otra cuenta");
        }
        user.setDni(request.dni());
        user.setDniVerified(true);
        return UserResponse.from(userRepository.save(user));
    }

    @Override
    @Transactional
    public void requestSellerVerification() {
        User user = getCurrentUser();
        long participated = userRepository.countAuctionsParticipatedByUser(user.getId());
        long won = userRepository.countAuctionsWonByUser(user.getId());
        if (participated < 5 || won < 3) {
            throw new InvalidOperationException(
                String.format("Requirements not met: participated=%d/5, won=%d/3", participated, won));
        }
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}
