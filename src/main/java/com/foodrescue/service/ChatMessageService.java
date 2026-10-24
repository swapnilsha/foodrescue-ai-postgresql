package com.foodrescue.service;

import com.foodrescue.entity.ChatMessage;
import java.util.List;

public interface ChatMessageService {
    ChatMessage saveMessage(String senderEmail, String recipientEmail, String content, String roomId);
    List<ChatMessage> getMessages(String roomId);
}
