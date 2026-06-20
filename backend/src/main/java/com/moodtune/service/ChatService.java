package com.moodtune.service;

import com.moodtune.dto.*;
import com.moodtune.entity.ChatSession;
import com.moodtune.entity.ChatMessage;
import com.moodtune.repository.ChatSessionRepository;
import com.moodtune.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final SongService songService;

    public ChatSessionDTO createSession(String title) {
        ChatSession session = new ChatSession();
        session.setTitle(title);
        ChatSession saved = sessionRepository.save(session);
        return toSessionDTO(saved);
    }

    public List<ChatSessionDTO> getAllSessions() {
        return sessionRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toSessionDTO)
                .collect(Collectors.toList());
    }

    public List<ChatMessageDTO> getSessionMessages(Long sessionId) {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(this::toMessageDTO)
                .collect(Collectors.toList());
    }

    private ChatSessionDTO toSessionDTO(ChatSession session) {
        ChatSessionDTO dto = new ChatSessionDTO();
        dto.setId(session.getId());
        dto.setTitle(session.getTitle());
        dto.setCreatedAt(session.getCreatedAt());
        dto.setUpdatedAt(session.getUpdatedAt());
        return dto;
    }

    private ChatMessageDTO toMessageDTO(ChatMessage msg) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId(msg.getId());
        dto.setSessionId(msg.getSessionId());
        dto.setSenderType(msg.getSenderType());
        dto.setTextContent(msg.getTextContent());
        dto.setCreatedAt(msg.getCreatedAt());
        if (msg.getSongId() != null) {
            try {
                dto.setSong(songService.getSongById(msg.getSongId()));
            } catch (Exception e) {
                // Song may have been deleted
            }
        }
        return dto;
    }
}
