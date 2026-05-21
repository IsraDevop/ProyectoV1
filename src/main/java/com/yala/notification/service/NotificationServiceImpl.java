package com.yala.notification.service;

import com.yala.exception.ForbiddenException;
import com.yala.exception.ResourceNotFoundException;
import com.yala.notification.dto.NotificationResponse;
import com.yala.notification.model.Notification;
import com.yala.notification.model.NotificationType;
import com.yala.notification.repository.NotificationRepository;
import com.yala.user.model.User;
import com.yala.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public void createNotification(Long userId, NotificationType type, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .message(message)
                .build();
        notificationRepository.save(notification);
        long unread = notificationRepository.countByUserIdAndIsReadFalse(userId);
        messagingTemplate.convertAndSend("/topic/notifications/" + userId, unread);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(Pageable pageable) {
        Long userId = getCurrentUserId();
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::from);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long notificationId) {
        Long userId = getCurrentUserId();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        if (!notification.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Cannot mark another user's notification");
        }
        notification.setIsRead(true);
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        Long userId = getCurrentUserId();
        notificationRepository.markAllAsReadByUserId(userId);
    }

    private Long getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"))
                .getId();
    }
}
