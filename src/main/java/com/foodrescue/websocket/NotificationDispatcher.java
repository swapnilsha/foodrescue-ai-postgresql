package com.foodrescue.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcher {

    private final SimpMessagingTemplate messagingTemplate;

    // Broadcast to all active NGOs when new food is available
    public void broadcastNewDonation(String foodTitle, double distanceKm) {
        String destination = "/topic/public.donations";
        Map<String, Object> payload = Map.of(
                "type", "NEW_DONATION",
                "message", "New food available: " + foodTitle,
                "distance", distanceKm
        );
        
        log.info("Broadcasting new donation alert to topic: {}", destination);
        messagingTemplate.convertAndSend(destination, payload);
    }

    // Send a private alert to a specific user (e.g., Food Expired, Request Accepted)
    public void sendPrivateNotification(String userEmail, String message) {
        String destination = "/queue/private-" + userEmail;
        Map<String, Object> payload = Map.of(
                "type", "PRIVATE_ALERT",
                "message", message
        );
        
        log.info("Sending private alert to user {}: {}", userEmail, message);
        messagingTemplate.convertAndSend(destination, payload);
    }
}