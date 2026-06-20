package com.moodtune.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moodtune.config.LlmConfig;
import com.moodtune.dto.LlmMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmClient {

    private final LlmConfig llmConfig;
    private final ObjectMapper objectMapper;

    /**
     * Streaming call - sends chunks to SseEmitter as they arrive.
     * Returns the full accumulated response text.
     */
    public String chatStream(List<LlmMessage> messages, SseEmitter emitter) throws IOException {
        WebClient client = buildClient();

        Map<String, Object> body = Map.of(
                "model", llmConfig.getModel(),
                "messages", messages,
                "stream", true
        );

        StringBuilder fullResponse = new StringBuilder();

        // Use WebClient's streaming capability
        String responseBody = client.post()
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // Parse SSE response lines
        if (responseBody != null) {
            for (String line : responseBody.split("\n")) {
                line = line.trim();
                if (line.startsWith("data: ") && !line.contains("[DONE]")) {
                    try {
                        String json = line.substring(6).trim();
                        JsonNode node = objectMapper.readTree(json);
                        String delta = node.path("choices").path(0).path("delta").path("content").asText("");
                        if (!delta.isEmpty()) {
                            fullResponse.append(delta);
                            emitter.send(SseEmitter.event()
                                    .name("message")
                                    .data(delta));
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse SSE chunk: {}", line, e);
                    }
                }
            }
        }

        return fullResponse.toString();
    }

    /**
     * Non-streaming call for simpler use cases.
     */
    public String chat(List<LlmMessage> messages) {
        WebClient client = buildClient();

        Map<String, Object> body = Map.of(
                "model", llmConfig.getModel(),
                "messages", messages,
                "stream", false
        );

        String responseBody = client.post()
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            JsonNode node = objectMapper.readTree(responseBody);
            return node.path("choices").path(0).path("message").path("content").asText("");
        } catch (Exception e) {
            log.error("Failed to parse LLM response", e);
            return "";
        }
    }

    private WebClient buildClient() {
        return WebClient.builder()
                .baseUrl(llmConfig.getApiUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + llmConfig.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
