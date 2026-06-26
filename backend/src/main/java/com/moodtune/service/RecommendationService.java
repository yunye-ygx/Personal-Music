package com.moodtune.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moodtune.dto.*;
import com.moodtune.entity.ChatMessage;
import com.moodtune.entity.Song;
import com.moodtune.mapper.ChatMessageMapper;
import com.moodtune.tool.SearchLikedSongsCallback;
import com.moodtune.tool.ToolCallCapture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
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
    private final SearchLikedSongsCallback searchLikedSongsCallback;

    /**
     * System prompt：要求 LLM 严格输出 JSON 格式
     * 后端从流式输出中提取 text 字段推给前端，流结束后解析 song 字段校验数据库
     */
    private static final String SYSTEM_PROMPT = """
            你是用户的私人音乐顾问和知心朋友。
            你的任务是根据用户当下的状态和心情，从他们的红心歌单中推荐歌曲。

            ## 工具使用（核心规则）

            当用户请求推荐歌曲时：
            1. **必须先调用 searchLikedSongs 工具**查询红心歌曲
            2. **只能推荐工具返回的歌曲**，绝不推荐工具未返回的歌曲
            3. 如果工具返回空结果，在 text 中告诉用户"红心歌单里暂时没有这类歌曲"，song 设为 null
            4. 根据用户描述提取关键词（歌名、歌手、风格、情绪）传给工具

            ## 输出格式（严格遵守）

            你的回复必须是合法 JSON，不得有任何 JSON 之外的内容：

            有推荐歌曲时：
            {"text":"你的自然语言回复内容","song":{"title":"歌名","artist":"歌手名"}}

            没有推荐歌曲时：
            {"text":"你的自然语言回复内容","song":null}

            注意：
            - text 内容中不要使用双引号，用单引号或顿号替代
            - text 内容不要换行，保持单行
            - 不要输出 ```json 代码块标记

            ## 推荐策略

            1. **理解真实需求**：用户说"伤心"时，可能想共鸣，也可能想振奋
            2. **情绪匹配优先**：优先考虑歌曲的情绪基调
            3. **避免重复**：聊天历史中已推荐的歌不要再推
            4. **引导明确**：需求模糊时反问澄清

            ## 回复风格

            像朋友一样自然聊天，简洁温暖，不要过度热情。

            当前时间：%s
            """;

    /**
     * Process user message, get recommendation from LLM, save messages.
     *
     * 单次调用策略：
     * - LLM 调用工具获取真实歌曲数据
     * - 流式输出自然语言回复
     * - 实时解析输出，提取歌名+歌手并校验数据库
     * - 只推送数据库中存在的歌曲
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

        // 2. Build message history
        List<LlmMessage> messages = buildMessageHistory(sessionId);

        // 3. 单次调用：流式 + 工具
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm EEEE"));
        List<LlmMessage> llmMessages = new ArrayList<>();
        llmMessages.add(new LlmMessage("system", String.format(SYSTEM_PROMPT, currentTime)));
        llmMessages.addAll(messages);

        ToolCallCapture toolCapture = new ToolCallCapture(searchLikedSongsCallback);
        List<ToolCallback> toolCallbacks = List.of(toolCapture);

        // 流式调用：后端实时提取 text 字段推给前端，返回完整原始 JSON
        String rawJson = llmClient.chatStreamWithToolsAndJsonExtract(llmMessages, emitter, toolCallbacks);

        log.info("LLM response complete, raw json length: {}, tool calls: {}",
                rawJson.length(), toolCapture.getCapturedResults().size());
        log.info("LLM raw json: {}", rawJson);

        // 4. 从完整 JSON 中解析 text 和 song，校验数据库
        RecommendationResult result = parseJsonResponse(rawJson);

        // 5. Save AI message
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

    private List<LlmMessage> buildMessageHistory(Long sessionId) {
        List<LlmMessage> messages = new ArrayList<>();
        List<ChatMessage> history = messageMapper.findBySessionIdOrderByCreatedAtAsc(sessionId);
        for (ChatMessage msg : history) {
            String role = "user".equals(msg.getSenderType()) ? "user" : "assistant";
            messages.add(new LlmMessage(role, msg.getTextContent()));
        }
        return messages;
    }

    /**
     * 从 LLM 返回的完整 JSON 中解析 text 和 song，并校验数据库
     */
    private RecommendationResult parseJsonResponse(String rawJson) {
        RecommendationResult result = new RecommendationResult();
        try {
            String json = rawJson.trim();
            if (json.startsWith("```json")) {
                json = json.substring(7);
                json = json.substring(0, json.lastIndexOf("```"));
            } else if (json.startsWith("```")) {
                json = json.substring(3);
                json = json.substring(0, json.lastIndexOf("```"));
            }
            json = json.trim();

            JsonNode node = objectMapper.readTree(json);
            String text = node.path("text").asText("");
            result.setText(text);

            JsonNode songNode = node.path("song");
            if (!songNode.isMissingNode() && !songNode.isNull()) {
                String title = songNode.path("title").asText(null);
                String artist = songNode.path("artist").asText(null);
                log.info("Extracted from JSON - title: '{}', artist: '{}'", title, artist);

                if (title != null && !title.isBlank()) {
                    Song dbSong = songService.findByTitleAndArtist(title, artist);
                    if (dbSong != null) {
                        log.info("Database lookup: FOUND (id={})", dbSong.getId());
                        result.setSong(SongDTO.builder()
                                .id(dbSong.getId())
                                .title(dbSong.getTitle())
                                .artist(dbSong.getArtist())
                                .genre(dbSong.getGenre())
                                .fileUrl(dbSong.getFileUrl())
                                .liked(dbSong.getLiked())
                                .moodTags(dbSong.getMoodTags())
                                .build());
                    } else {
                        log.warn("Song not found in DB: {} - {}", title, artist);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse JSON response, using raw text: {}", e.getMessage());
            result.setText(rawJson);
        }
        return result;
    }
}
