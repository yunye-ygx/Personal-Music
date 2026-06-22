package com.moodtune.service;

import com.moodtune.dto.*;
import com.moodtune.entity.ChatSession;
import com.moodtune.entity.ChatMessage;
import com.moodtune.mapper.ChatSessionMapper;
import com.moodtune.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final SongService songService;

    public ChatSessionDTO createSession(String title) {
        ChatSession session = ChatSession.builder()
                .title(title)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        sessionMapper.insert(session);
        return toSessionDTO(session);
    }

    public List<ChatSessionDTO> getAllSessions() {
        return sessionMapper.findAllByOrderByUpdatedAtDesc().stream()
                .map(session -> {
                    ChatSessionDTO dto = toSessionDTO(session);
                    // 获取最后一条消息作为预览
                    List<ChatMessage> messages = messageMapper.findBySessionIdOrderByCreatedAtAsc(session.getId());
                    if (!messages.isEmpty()) {
                        ChatMessage lastMsg = messages.get(messages.size() - 1);
                        String preview = lastMsg.getTextContent();
                        if (preview != null && preview.length() > 30) {
                            preview = preview.substring(0, 30) + "...";
                        }
                        dto.setLastMessage(preview);
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public void deleteSession(Long sessionId) {
        // 删除会话的所有消息
        messageMapper.deleteBySessionId(sessionId);
        // 删除会话
        sessionMapper.deleteById(sessionId);
    }

    public List<ChatMessageDTO> getSessionMessages(Long sessionId) {
        return messageMapper.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
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
