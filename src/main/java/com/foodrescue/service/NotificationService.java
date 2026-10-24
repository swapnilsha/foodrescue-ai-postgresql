package com.foodrescue.service;

import com.foodrescue.dto.NotificationResponse;
import com.foodrescue.entity.FoodDonation;
import com.foodrescue.entity.User;

import java.util.List;

public interface NotificationService {
    List<NotificationResponse> getNotificationsForUser(User user);
    long getUnreadCount(User user);
    void markAsRead(Long notificationId, User user);
    void markAllAsRead(User user);
    void createNotification(User user, String message, String type, FoodDonation donation);
}
