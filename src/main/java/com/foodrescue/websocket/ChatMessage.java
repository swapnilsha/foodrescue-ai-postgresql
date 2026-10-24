package com.foodrescue.websocket;

import lombok.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Payload exchanged over WebSocket for chat messages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    public enum Type { CHAT, JOIN, LEAVE }

    private Type type;
    private String sender;      // display name
    private String senderEmail; // used to route private messages
    private String content;
    private String roomId;      // conversation room (e.g. "donor-123-ngo-456")
    private String timestamp;   // formatted HH:mm

    /** Called before send to stamp the current time. */
    public void stampTime() {
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
