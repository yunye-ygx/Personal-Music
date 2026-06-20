package com.moodtune.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatMessageDTO {
    private Long id;
    private Long sessionId;
    private String senderType;
    private String textContent;
    private SongDTO song; // populated when songId is not null
    private LocalDateTime createdAt;
}
