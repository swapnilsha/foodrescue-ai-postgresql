package com.foodrescue.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * Handles real-time chat messages over WebSocket/STOMP.
 *
 * Client sends to:  /app/chat.send
 * Server routes to: /topic/chat.{roomId}  (all room members receive it)
 */
@Controller
@Slf4j
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final com.foodrescue.service.ChatMessageService chatMessageService;
    private final com.foodrescue.repository.UserRepository userRepository;

    public ChatController(SimpMessagingTemplate messagingTemplate,
                          com.foodrescue.service.ChatMessageService chatMessageService,
                          com.foodrescue.repository.UserRepository userRepository) {
        this.messagingTemplate = messagingTemplate;
        this.chatMessageService = chatMessageService;
        this.userRepository = userRepository;
    }

    /**
     * Receives a chat message and broadcasts it to all subscribers in that room.
     * Client sends:  SEND /app/chat.send  { type, content, roomId }
     * Server pushes: /topic/chat.{roomId}
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage message, SimpMessageHeaderAccessor headerAccessor) {
        Principal principal = headerAccessor.getUser();
        if (principal != null) {
            message.setSenderEmail(principal.getName());
            if (message.getSender() == null || message.getSender().isBlank()) {
                message.setSender(principal.getName());
            }
        }

        message.setType(ChatMessage.Type.CHAT);
        message.stampTime();

        // Deduce recipient email based on the roomId (room-{min_id}-{max_id})
        String recipientEmail = "system@foodrescue.com";
        try {
            String peerIdStr = message.getRoomId().replace("room-", "");
            if (peerIdStr.contains("-")) {
                String[] parts = peerIdStr.split("-");
                long id1 = Long.parseLong(parts[0]);
                long id2 = Long.parseLong(parts[1]);
                
                String senderEmail = message.getSenderEmail() != null ? message.getSenderEmail() : message.getSender();
                java.util.Optional<com.foodrescue.entity.User> senderOpt = userRepository.findByEmail(senderEmail);
                long recipientId = id2;
                if (senderOpt.isPresent()) {
                    long senderId = senderOpt.get().getId();
                    recipientId = (senderId == id1) ? id2 : id1;
                }
                
                java.util.Optional<com.foodrescue.entity.User> peerOpt = userRepository.findById(recipientId);
                if (peerOpt.isPresent()) {
                    recipientEmail = peerOpt.get().getEmail();
                }
            } else {
                Long peerId = Long.parseLong(peerIdStr);
                java.util.Optional<com.foodrescue.entity.User> peerOpt = userRepository.findById(peerId);
                if (peerOpt.isPresent()) {
                    recipientEmail = peerOpt.get().getEmail();
                }
            }
        } catch (Exception e) {
            log.warn("Could not deduce recipient email for roomId {}: {}", message.getRoomId(), e.getMessage());
        }

        // Save to Database
        chatMessageService.saveMessage(
            message.getSenderEmail() != null ? message.getSenderEmail() : message.getSender(),
            recipientEmail,
            message.getContent(),
            message.getRoomId()
        );

        String roomTopic = "/topic/chat." + sanitizeRoomId(message.getRoomId());
        log.debug("Chat message from {} → {}: {}", message.getSender(), roomTopic, message.getContent());

        messagingTemplate.convertAndSend(roomTopic, message);
    }

    /**
     * User joins a chat room — broadcasts a join notice to the room.
     * Client sends: SEND /app/chat.join  { roomId, sender }
     */
    @MessageMapping("/chat.join")
    public void joinRoom(@Payload ChatMessage message, SimpMessageHeaderAccessor headerAccessor) {
        Principal principal = headerAccessor.getUser();
        if (principal != null) {
            message.setSenderEmail(principal.getName());
            if (message.getSender() == null || message.getSender().isBlank()) {
                message.setSender(principal.getName());
            }
        }

        message.setType(ChatMessage.Type.JOIN);
        message.setContent(message.getSender() + " joined the conversation.");
        message.stampTime();

        String roomTopic = "/topic/chat." + sanitizeRoomId(message.getRoomId());
        messagingTemplate.convertAndSend(roomTopic, message);
    }

    // Prevent path-traversal in room IDs
    private String sanitizeRoomId(String roomId) {
        if (roomId == null || roomId.isBlank()) return "general";
        return roomId.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }
}
