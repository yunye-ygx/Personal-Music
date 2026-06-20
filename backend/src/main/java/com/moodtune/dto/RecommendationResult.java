package com.moodtune.dto;

import lombok.Data;

@Data
public class RecommendationResult {
    private String text;
    private Long songId;
}
