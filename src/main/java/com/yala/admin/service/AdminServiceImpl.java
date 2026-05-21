package com.yala.admin.service;

import com.yala.event.SellerVerifiedEvent;
import com.yala.event.StoreApprovedEvent;
import com.yala.exception.ResourceNotFoundException;
import com.yala.user.dto.UserResponse;
import com.yala.user.model.Role;
import com.yala.user.model.User;
import com.yala.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public UserResponse approveStore(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setRole(Role.SELLER);
        user.setIsVerifiedSeller(true);
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new StoreApprovedEvent(this, saved.getId(), saved.getEmail(), saved.getName()));
        return UserResponse.from(saved);
    }

    @Override
    @Transactional
    public UserResponse approveSellerVerification(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setRole(Role.SELLER);
        user.setIsVerifiedSeller(true);
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new SellerVerifiedEvent(this, saved.getId()));
        return UserResponse.from(saved);
    }
}
