package com.moodtune.controller;

import com.moodtune.dto.*;
import com.moodtune.service.ChatService;
import com.moodtune.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final RecommendationService recommendationService;

    @PostMapping("/sessions")
    public ResponseEntity<ChatSessionDTO> createSession(@RequestBody Map<String, String> body) {
        String title = body.getOrDefault("title", "新会话");
        return ResponseEntity.ok(chatService.createSession(title));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionDTO>> getSessions() {
        return ResponseEntity.ok(chatService.getAllSessions());
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<ChatMessageDTO>> getMessages(@PathVariable Long sessionId) {
        return ResponseEntity.ok(chatService.getSessionMessages(sessionId));
    }

    @PostMapping(value = "/sessions/{sessionId}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessage(@PathVariable Long sessionId, @RequestBody Map<String, String> body) {
        String message = body.get("message");
        SseEmitter emitter = new SseEmitter(60000L); // 60s timeout

        // Run in async thread to not block
        new Thread(() -> {
            try {
                RecommendationResult result = recommendationService.recommend(sessionId, message, emitter);

                // Send final event with structured data
                emitter.send(SseEmitter.event()
                        .name("recommendation")
                        .data(result));

                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }
}
