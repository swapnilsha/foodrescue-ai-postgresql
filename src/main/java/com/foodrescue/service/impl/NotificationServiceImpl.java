package com.foodrescue.service.impl;

import com.foodrescue.dto.NotificationResponse;
import com.foodrescue.entity.FoodDonation;
import com.foodrescue.entity.Notification;
import com.foodrescue.entity.User;
import com.foodrescue.repository.NotificationRepository;
import com.foodrescue.service.NotificationService;
import com.foodrescue.websocket.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationDispatcher notificationDispatcher;

    @Override
    public List<NotificationResponse> getNotificationsForUser(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, User user) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUser().getId().equals(user.getId())) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    @Override
    @Transactional
    public void markAllAsRead(User user) {
        notificationRepository.markAllAsReadForUser(user);
    }

    @Override
    @Transactional
    public void createNotification(User user, String message, String type, FoodDonation donation) {
        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .type(type)
                .donation(donation)
                .isRead(false)
                .build();
        notificationRepository.save(notification);

        // Broadcast real-time via WebSocket
        notificationDispatcher.sendPrivateNotification(user.getEmail(), message);
        log.info("Notification created for user {}: {}", user.getEmail(), message);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .message(n.getMessage())
                .type(n.getType())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .donationId(n.getDonation() != null ? n.getDonation().getId() : null)
                .build();
    }
}
