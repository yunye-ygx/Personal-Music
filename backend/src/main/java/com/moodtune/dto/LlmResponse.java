package com.moodtune.dto;

import lombok.Data;
import java.util.List;

@Data
public class LlmResponse {
    private List<Choice> choices;

    @Data
    public static class Choice {
        private LlmMessage message;
        private Delta delta;
    }

    @Data
    public static class Delta {
        private String content;
    }
}
