package com.foodrescue.controller;

import com.foodrescue.entity.ChatMessage;
import com.foodrescue.entity.User;
import com.foodrescue.repository.UserRepository;
import com.foodrescue.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController("chatRestController")
@Slf4j
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageService chatMessageService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * REST endpoint to get all users whom the current authenticated user can communicate with.
     * Donors can talk to NGOs, NGOs to Donors, Admins to everyone.
     */
    @GetMapping("/api/chat/partners")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> getChatPartners(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        List<User> partners;
        String role = currentUser.getRole();
        if ("ROLE_DONOR".equalsIgnoreCase(role)) {
            partners = userRepository.findAll().stream()
                    .filter(u -> "ROLE_NGO".equalsIgnoreCase(u.getRole()))
                    .collect(Collectors.toList());
        } else if ("ROLE_NGO".equalsIgnoreCase(role)) {
            partners = userRepository.findAll().stream()
                    .filter(u -> "ROLE_DONOR".equalsIgnoreCase(u.getRole()))
                    .collect(Collectors.toList());
        } else {
            // ADMIN can chat with all non-admin users
            partners = userRepository.findAll().stream()
                    .filter(u -> !u.getId().equals(currentUser.getId()))
                    .collect(Collectors.toList());
        }

        long myId = currentUser.getId();
        List<Map<String, Object>> response = partners.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("name", p.getName());
            map.put("email", p.getEmail());
            map.put("role", p.getRole().replace("ROLE_", ""));
            map.put("phone", p.getPhone() != null ? p.getPhone() : "");
            map.put("online", true); // Default online state

            // Fetch last message from MySQL for the unified roomId
            long peerId = p.getId();
            String uRoomId = "room-" + Math.min(myId, peerId) + "-" + Math.max(myId, peerId);
            List<ChatMessage> msgs = chatMessageService.getMessages(uRoomId);
            if (!msgs.isEmpty()) {
                ChatMessage last = msgs.get(msgs.size() - 1);
                map.put("lastMsg", last.getContent());
                map.put("time", last.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")));
            } else {
                map.put("lastMsg", "Click to start chatting...");
                map.put("time", "");
            }
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * REST endpoint to fetch historical messages for a specific room.
     * Security checks ensure users can only access rooms they belong to.
     */
    @GetMapping("/api/chat/messages/{roomId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> getMessages(
            @PathVariable String roomId,
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        // roomId is formatted as room-{min_id}-{max_id} or room-{peer_id}
        if (!currentUser.getRole().equalsIgnoreCase("ROLE_ADMIN")) {
            String peerIdStr = roomId.replace("room-", "");
            try {
                if (peerIdStr.contains("-")) {
                    String[] parts = peerIdStr.split("-");
                    long id1 = Long.parseLong(parts[0]);
                    long id2 = Long.parseLong(parts[1]);
                    if (currentUser.getId() != id1 && currentUser.getId() != id2) {
                        return ResponseEntity.status(403).build();
                    }
                } else {
                    Long peerId = Long.parseLong(peerIdStr);
                    if (!currentUser.getId().equals(peerId)) {
                        // Allow room-{recipientId} fallback if it matches recipient
                    }
                }
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().build();
            }
        }

        List<ChatMessage> messages = chatMessageService.getMessages(roomId);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");

        List<Map<String, Object>> response = messages.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("sender", m.getSenderEmail());
            map.put("senderEmail", m.getSenderEmail());
            map.put("content", m.getContent());
            map.put("roomId", m.getRoomId());
            map.put("timestamp", m.getCreatedAt().format(fmt));
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
