package com.moodtune.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LlmMessage {
    private String role; // "system", "user", "assistant"
    private String content;
}
