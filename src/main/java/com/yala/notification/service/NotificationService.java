package com.yala.notification.service;

import com.yala.notification.dto.NotificationResponse;
import com.yala.notification.model.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    void createNotification(Long userId, NotificationType type, String message);
    Page<NotificationResponse> getMyNotifications(Pageable pageable);
    NotificationResponse markAsRead(Long notificationId);
    void markAllAsRead();
}
