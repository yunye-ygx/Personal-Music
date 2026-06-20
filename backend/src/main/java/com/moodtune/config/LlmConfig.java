package com.moodtune.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "llm")
public class LlmConfig {
    private String apiUrl;
    private String apiKey;
    private String model;
}
