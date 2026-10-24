package com.foodrescue.service.impl;

import com.foodrescue.entity.ChatMessage;
import com.foodrescue.repository.ChatMessageRepository;
import com.foodrescue.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;

    @Override
    @Transactional
    public ChatMessage saveMessage(String senderEmail, String recipientEmail, String content, String roomId) {
        ChatMessage message = ChatMessage.builder()
                .senderEmail(senderEmail)
                .recipientEmail(recipientEmail)
                .content(content)
                .roomId(roomId)
                .build();
        ChatMessage saved = chatMessageRepository.save(message);
        log.info("[CHAT] Message saved in room {}: {}", roomId, content);
        return saved;
    }

    @Override
    public List<ChatMessage> getMessages(String roomId) {
        return chatMessageRepository.findByRoomIdOrderByCreatedAtAsc(roomId);
    }
}
