package com.moodtune.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moodtune.dto.*;
import com.moodtune.entity.ChatMessage;
import com.moodtune.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final LlmClient llmClient;
    private final SongService songService;
    private final ChatMessageMapper messageMapper;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            你是一个私人音乐顾问，是用户的知心朋友。
            你的任务是根据用户当下的状态和心情，从他们的红心歌单中推荐歌曲。

            ## 工具使用指南

            使用 searchLikedSongs 工具查询用户的红心歌曲：
            - 根据用户的描述提取关键信息（歌名、歌手、风格、情绪）
            - 调用工具时，提供尽可能明确的参数
            - 从查询结果中选择最合适的一首歌

            ## 推荐策略

            1. **理解真实需求**：用户说"伤心"时，可能想要共鸣的歌，也可能想要振奋的歌。根据上下文判断。
            2. **情绪匹配优先**：优先考虑歌曲的情绪基调（通过moodTags和歌词主题）
            3. **避免重复推荐**：如果用户说"换一首"，避免推荐聊天历史中已推荐的歌
            4. **空结果处理**：如果工具返回空列表，告诉用户"你的红心歌单里暂时没有这类歌曲"
            5. **引导明确需求**：如果用户需求模糊，可以问"你是想找首歌陪你难过，还是想听点振奋的？"

            ## 特殊场景

            - 用户说"给我推荐首歌"且没有更多上下文：调用 searchLikedSongs() 不传任何参数，从所有红心歌曲中随机选一首
            - 红心歌单为空（工具返回空列表且用户是第一次询问）：提示"你还没有收藏任何歌曲哦，先去添加几首红心歌曲吧～"
            - 工具调用失败（抛出异常）：告知用户"抱歉，查询歌曲时出了点问题，请稍后再试"

            当前时间：%s

            ## 返回格式（严格JSON）

            {
              "text": "你的回复和推荐理由（自然、像朋友一样）",
              "song": {
                "id": 123,
                "title": "歌名",
                "artist": "歌手",
                "genre": "风格",
                "fileUrl": "播放地址",
                "moodTags": ["标签1", "标签2"]
              }
            }

            如果是纯对话不推荐歌曲，song字段设为null。
            """;

    /**
     * Process user message, get recommendation from LLM, save messages.
     */
    public RecommendationResult recommend(Long sessionId, String userMessage, SseEmitter emitter) throws IOException {
        // 1. Save user message
        ChatMessage userChatMsg = ChatMessage.builder()
                .sessionId(sessionId)
                .senderType("user")
                .textContent(userMessage)
                .createdAt(LocalDateTime.now())
                .build();
        messageMapper.insert(userChatMsg);

        // 2. Build system prompt (without song list)
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm EEEE"));
        String systemPrompt = String.format(SYSTEM_PROMPT, currentTime);

        // 3. Build message history
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage("system", systemPrompt));

        List<ChatMessage> history = messageMapper.findBySessionIdOrderByCreatedAtAsc(sessionId);
        for (ChatMessage msg : history) {
            String role = "user".equals(msg.getSenderType()) ? "user" : "assistant";
            messages.add(new LlmMessage(role, msg.getTextContent()));
        }

        // 4. Call LLM with streaming (Function Calling automatic)
        String fullResponse = llmClient.chatStream(messages, emitter);

        // 5. Parse structured response
        RecommendationResult result = parseResponse(fullResponse);

        // 6. Save AI message
        ChatMessage aiChatMsg = ChatMessage.builder()
                .sessionId(sessionId)
                .senderType("ai")
                .textContent(result.getText())
                .songId(result.getSong() != null ? result.getSong().getId() : null)
                .createdAt(LocalDateTime.now())
                .build();
        messageMapper.insert(aiChatMsg);

        return result;
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

            // Parse song object (not just song_id)
            if (node.has("song") && !node.get("song").isNull()) {
                JsonNode songNode = node.get("song");
                SongDTO song = SongDTO.builder()
                        .id(songNode.path("id").asLong())
                        .title(songNode.path("title").asText())
                        .artist(songNode.path("artist").asText())
                        .genre(songNode.path("genre").asText())
                        .fileUrl(songNode.path("fileUrl").asText())
                        .moodTags(parseMoodTags(songNode.path("moodTags")))
                        .build();
                result.setSong(song);
            } else {
                result.setSong(null);
            }
        } catch (Exception e) {
            log.warn("Failed to parse LLM response as JSON, using raw text: {}", e.getMessage());
            result.setText(response);
            result.setSong(null);
        }
        return result;
    }

    private List<String> parseMoodTags(JsonNode moodTagsNode) {
        if (moodTagsNode.isArray()) {
            List<String> tags = new ArrayList<>();
            moodTagsNode.forEach(tag -> tags.add(tag.asText()));
            return tags;
        }
        return null;
    }
}
