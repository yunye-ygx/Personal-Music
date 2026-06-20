package com.moodtune.repository;

import com.moodtune.entity.ChatSession;
import com.moodtune.entity.ChatMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ChatRepositoryTest {

    @Autowired
    private ChatSessionRepository sessionRepository;

    @Autowired
    private ChatMessageRepository messageRepository;

    @Test
    void createSessionAndMessages() {
        ChatSession session = new ChatSession();
        session.setTitle("今天好累");
        ChatSession savedSession = sessionRepository.save(session);
        assertNotNull(savedSession.getId());

        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(savedSession.getId());
        userMsg.setSenderType("user");
        userMsg.setTextContent("今天好累，想听点歌");
        messageRepository.save(userMsg);

        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setSessionId(savedSession.getId());
        aiMsg.setSenderType("ai");
        aiMsg.setTextContent("给你推荐一首安静的歌");
        aiMsg.setSongId(1L);
        messageRepository.save(aiMsg);

        List<ChatMessage> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(savedSession.getId());
        assertEquals(2, messages.size());
        assertEquals("user", messages.get(0).getSenderType());
        assertEquals("ai", messages.get(1).getSenderType());
        assertNotNull(messages.get(1).getSongId());
    }
}
