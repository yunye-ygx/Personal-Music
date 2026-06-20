package com.moodtune.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moodtune.dto.*;
import com.moodtune.entity.ChatMessage;
import com.moodtune.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final LlmClient llmClient;
    private final SongService songService;
    private final ChatMessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            你是一个私人音乐顾问，是用户的知心朋友。
            你的任务是根据用户当下的状态和心情，从他们的歌单中推荐一首最合适的歌。

            你的歌单如下：
            %s

            当前时间：%s

            规则：
            1. 根据对话内容理解用户的心情和需求
            2. 从歌单中选择最匹配的歌曲
            3. 推荐理由要自然，像朋友推荐一样
            4. 如果用户说"换一首"，推荐不同的歌曲
            5. 你的回复必须是JSON格式：{"text": "你的回复文字", "song_id": 歌曲ID}
            6. song_id 必须是歌单中某首歌的ID，如果是纯对话不需要推荐歌曲则为 null
            7. text 中包含推荐理由，自然地融入心情和场景描述
            """;

    /**
     * Process user message, get recommendation from LLM, save messages.
     */
    public RecommendationResult recommend(Long sessionId, String userMessage, SseEmitter emitter) throws IOException {
        // 1. Save user message
        ChatMessage userChatMsg = new ChatMessage();
        userChatMsg.setSessionId(sessionId);
        userChatMsg.setSenderType("user");
        userChatMsg.setTextContent(userMessage);
        messageRepository.save(userChatMsg);

        // 2. Build song list for system prompt
        String songList = buildSongList();
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm EEEE"));
        String systemPrompt = String.format(SYSTEM_PROMPT, songList, currentTime);

        // 3. Build message history
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage("system", systemPrompt));

        List<ChatMessage> history = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        for (ChatMessage msg : history) {
            String role = "user".equals(msg.getSenderType()) ? "user" : "assistant";
            messages.add(new LlmMessage(role, msg.getTextContent()));
        }

        // 4. Call LLM with streaming
        String fullResponse = llmClient.chatStream(messages, emitter);

        // 5. Parse structured response
        RecommendationResult result = parseResponse(fullResponse);

        // 6. Save AI message
        ChatMessage aiChatMsg = new ChatMessage();
        aiChatMsg.setSessionId(sessionId);
        aiChatMsg.setSenderType("ai");
        aiChatMsg.setTextContent(result.getText());
        aiChatMsg.setSongId(result.getSongId());
        messageRepository.save(aiChatMsg);

        return result;
    }

    private String buildSongList() {
        return songService.getAllSongs().stream()
                .map(s -> String.format("ID:%d | %s - %s | 风格:%s | 喜欢:%s",
                        s.getId(), s.getTitle(), s.getArtist(),
                        s.getGenre() != null ? s.getGenre() : "未知",
                        s.getLiked() ? "是" : "否"))
                .collect(Collectors.joining("\n"));
    }

    private RecommendationResult parseResponse(String response) {
        RecommendationResult result = new RecommendationResult();
        try {
            String jsonStr = response.trim();
            // Handle case where LLM wraps JSON in markdown code block
            if (jsonStr.contains("```json")) {
                jsonStr = jsonStr.substring(jsonStr.indexOf("```json") + 7);
                jsonStr = jsonStr.substring(0, jsonStr.lastIndexOf("```"));
            } else if (jsonStr.contains("```")) {
                jsonStr = jsonStr.substring(jsonStr.indexOf("```") + 3);
                jsonStr = jsonStr.substring(0, jsonStr.lastIndexOf("```"));
            }
            jsonStr = jsonStr.trim();

            JsonNode node = objectMapper.readTree(jsonStr);
            result.setText(node.path("text").asText(response));
            result.setSongId(node.has("song_id") && !node.get("song_id").isNull()
                    ? node.get("song_id").asLong() : null);
        } catch (Exception e) {
            log.warn("Failed to parse LLM response as JSON, using raw text: {}", e.getMessage());
            result.setText(response);
            result.setSongId(null);
        }
        return result;
    }
}
