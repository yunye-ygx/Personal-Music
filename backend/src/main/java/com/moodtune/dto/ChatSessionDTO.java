package com.moodtune.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatSessionDTO {
    private Long id;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
