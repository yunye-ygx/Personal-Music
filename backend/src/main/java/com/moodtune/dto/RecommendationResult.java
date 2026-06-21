package com.moodtune.dto;

import lombok.Data;

@Data
public class RecommendationResult {
    private String text;
    private Long songId;  // Deprecated: will be replaced by song field in Task 5
    private SongDTO song;
}
