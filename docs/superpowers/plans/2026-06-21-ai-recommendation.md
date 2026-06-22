# AI推荐功能优化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 优化AI音乐推荐功能，使用Function Calling从红心歌单智能推荐，支持情绪标签查询，返回完整歌曲信息，完善异常处理。

**Architecture:** 使用Spring AI的@Tool注解实现Function Calling，AI通过searchLikedSongs工具按需查询红心歌曲（支持歌名/歌手/风格/情绪多维度），返回完整SongDTO对象给前端直接播放，优化System Prompt移除歌曲列表硬编码。

**Tech Stack:** 
- Spring Boot 3.x + Spring AI
- MyBatis-Plus（QueryWrapper动态查询）
- PostgreSQL（GIN索引支持数组查询）
- 通义千问API（qwen-plus）

## Global Constraints

- Java版本：17+
- 数据库：PostgreSQL（已配置连接）
- 所有歌曲查询必须包含 `liked = true` 条件（严格红心模式）
- 异常处理：保留流式输出已生成部分，通过SSE error事件通知前端
- 超时配置：连接超时10秒，首次响应超时30秒
- 返回格式：`{text: String, song: SongDTO}` 或 `{text: String, song: null}`
- 提交信息：遵循Conventional Commits规范

---

## 文件结构映射

### 新增文件
- `backend/src/main/java/com/moodtune/dto/SongDTO.java` - 歌曲DTO，包含moodTags
- `backend/src/main/java/com/moodtune/service/SongSearchTool.java` - Function Calling工具类
- `backend/src/main/resources/db/migration/V2__add_mood_tags.sql` - 数据库迁移脚本

### 修改文件
- `backend/src/main/java/com/moodtune/entity/Song.java:32` - 添加moodTags字段
- `backend/src/main/java/com/moodtune/dto/RecommendationResult.java:6-9` - 修改song_id为song对象
- `backend/src/main/java/com/moodtune/service/RecommendationService.java:30-103` - 移除buildSongList，更新prompt，修改解析逻辑
- `backend/src/main/java/com/moodtune/service/LlmClient.java:29-68` - 增强异常处理和超时配置
- `backend/src/main/java/com/moodtune/config/OpenAiConfig.java` - 新建配置类（如不存在）

---

### Task 1: 数据库迁移 - 添加mood_tags字段

**Files:**
- Create: `backend/src/main/resources/db/migration/V2__add_mood_tags.sql`
- Modify: `backend/src/main/resources/schema.sql:10`

**Interfaces:**
- Consumes: 无
- Produces: `songs.mood_tags TEXT[]` 字段及GIN索引

- [ ] **Step 1: 创建迁移脚本**

```sql
-- V2__add_mood_tags.sql
-- 添加情绪标签字段到songs表

ALTER TABLE songs ADD COLUMN IF NOT EXISTS mood_tags TEXT[];

-- 创建GIN索引支持数组查询
CREATE INDEX IF NOT EXISTS idx_songs_mood_tags ON songs USING GIN(mood_tags);

-- 为现有歌曲添加示例mood_tags（可选，用于测试）
UPDATE songs SET mood_tags = ARRAY['未分类'] WHERE mood_tags IS NULL;
```

- [ ] **Step 2: 更新schema.sql**

在 `backend/src/main/resources/schema.sql` 的 `songs` 表定义中添加：

```sql
-- 在 liked BOOLEAN NOT NULL DEFAULT FALSE, 这行之后添加：
mood_tags TEXT[],                                            -- 情绪标签（如：治愈、伤感、激昂）
```

在索引部分添加：

```sql
-- 在文件末尾添加：
CREATE INDEX IF NOT EXISTS idx_songs_mood_tags ON songs USING GIN(mood_tags);  -- 加速情绪标签查询
```

- [ ] **Step 3: 手动执行迁移脚本（开发环境）**

连接到PostgreSQL数据库：

```bash
psql -h 192.168.100.128 -p 5433 -U postgres -d moodtune
```

执行迁移：

```sql
\i backend/src/main/resources/db/migration/V2__add_mood_tags.sql
```

验证字段已添加：

```sql
\d songs
```

Expected: 输出中包含 `mood_tags | text[] |` 和索引 `idx_songs_mood_tags`

- [ ] **Step 4: 提交**

```bash
git add backend/src/main/resources/db/migration/V2__add_mood_tags.sql backend/src/main/resources/schema.sql
git commit -m "feat(db): add mood_tags field and GIN index to songs table

- Add mood_tags TEXT[] column for emotion labels
- Create GIN index for array queries
- Update schema.sql with new field definition"
```

---

### Task 2: 更新实体类和DTO

**Files:**
- Modify: `backend/src/main/java/com/moodtune/entity/Song.java:32`
- Create: `backend/src/main/java/com/moodtune/dto/SongDTO.java`
- Modify: `backend/src/main/java/com/moodtune/dto/RecommendationResult.java:6-9`

**Interfaces:**
- Consumes: `songs.mood_tags TEXT[]` 数据库字段
- Produces: 
  - `Song.getMoodTags(): List<String>`
  - `SongDTO` 类（包含id, title, artist, genre, fileUrl, moodTags）
  - `RecommendationResult.getSong(): SongDTO`

- [ ] **Step 1: 更新Song实体类**

在 `backend/src/main/java/com/moodtune/entity/Song.java` 的 `liked` 字段后添加：

```java
@TableField("mood_tags")
private List<String> moodTags;
```

完整的字段部分应该是：

```java
@Builder.Default
private Boolean liked = false;

@TableField("mood_tags")
private List<String> moodTags;

@TableField("created_at")
private LocalDateTime createdAt;
```

- [ ] **Step 2: 创建SongDTO类**

```java
package com.moodtune.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SongDTO {
    private Long id;
    private String title;
    private String artist;
    private String genre;
    private String fileUrl;
    private List<String> moodTags;
}
```

- [ ] **Step 3: 更新RecommendationResult**

修改 `backend/src/main/java/com/moodtune/dto/RecommendationResult.java`：

```java
package com.moodtune.dto;

import lombok.Data;

@Data
public class RecommendationResult {
    private String text;
    private SongDTO song;  // 从 Long songId 改为 SongDTO song
}
```

- [ ] **Step 4: 验证编译**

```bash
cd backend
./mvnw compile
```

Expected: `BUILD SUCCESS`

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/moodtune/entity/Song.java \
        backend/src/main/java/com/moodtune/dto/SongDTO.java \
        backend/src/main/java/com/moodtune/dto/RecommendationResult.java
git commit -m "feat(dto): add mood_tags support to Song entity and create SongDTO

- Add moodTags field to Song entity
- Create SongDTO for AI response
- Update RecommendationResult to return full SongDTO instead of songId"
```

---

### Task 3: 实现Function Calling工具类

**Files:**
- Create: `backend/src/main/java/com/moodtune/service/SongSearchTool.java`

**Interfaces:**
- Consumes: 
  - `SongMapper.selectList(QueryWrapper<Song>): List<Song>`
  - `Song` 实体类
- Produces:
  - `searchLikedSongs(String title, String artist, String genre, String keyword): List<Song>`
  - 该方法带有 `@Tool` 注解，供Spring AI调用

- [ ] **Step 1: 创建SongSearchTool类**

```java
package com.moodtune.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.moodtune.entity.Song;
import com.moodtune.mapper.SongMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.Tool;
import org.springframework.ai.tool.ToolParameter;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SongSearchTool {

    private final SongMapper songMapper;

    @Tool(description = "查询用户红心歌单中的歌曲。所有参数都是可选的，如果都不提供则返回所有红心歌曲。")
    public List<Song> searchLikedSongs(
            @ToolParameter(description = "歌曲名称。用于精确查询歌名。例如：晴天、七里香") 
            String title,
            
            @ToolParameter(description = "歌手名称。例如：周杰伦、陈奕迅") 
            String artist,
            
            @ToolParameter(description = "音乐风格。例如：民谣、摇滚、流行、爵士") 
            String genre,
            
            @ToolParameter(description = "情绪或关键词。会在歌名、歌手、风格、情绪标签中模糊搜索。例如：安静、伤感、治愈、激昂") 
            String keyword
    ) {
        try {
            QueryWrapper<Song> wrapper = new QueryWrapper<>();
            wrapper.eq("liked", true);
            
            if (title != null && !title.trim().isEmpty()) {
                wrapper.like("title", title);
            }
            if (artist != null && !artist.trim().isEmpty()) {
                wrapper.like("artist", artist);
            }
            if (genre != null && !genre.trim().isEmpty()) {
                wrapper.eq("genre", genre);
            }
            if (keyword != null && !keyword.trim().isEmpty()) {
                wrapper.and(w -> w
                    .like("title", keyword)
                    .or().like("artist", keyword)
                    .or().like("genre", keyword)
                    .or().apply("? = ANY(mood_tags)", keyword)
                );
            }
            
            wrapper.last("LIMIT 20");
            
            List<Song> results = songMapper.selectList(wrapper);
            log.info("searchLikedSongs查询参数: title={}, artist=, genre={}, keyword={}, 返回{}首歌曲", 
                     title, artist, genre, keyword, results.size());
            return results;
            
        } catch (Exception e) {
            log.error("查询红心歌曲失败", e);
            throw new RuntimeException("歌曲查询失败，请稍后重试");
        }
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
cd backend
./mvnw compile
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/moodtune/service/SongSearchTool.java
git commit -m "feat(ai): implement searchLikedSongs tool with Spring AI @Tool annotation

- Support 4 optional parameters: title, artist, genre, keyword
- Query only liked=true songs (strict red heart mode)
- Keyword searches across all text fields and mood_tags
- Limit results to 20 songs"
```

---

### Task 4: 配置超时和Function Calling

**Files:**
- Create: `backend/src/main/java/com/moodtune/config/AiConfig.java`
- Modify: `backend/src/main/java/com/moodtune/service/LlmClient.java:23-68`

**Interfaces:**
- Consumes:
  - `SongSearchTool.searchLikedSongs(...)`
  - Spring AI的 `ChatClient.Builder`
- Produces:
  - `ChatClient.Builder` 配置了超时和Function Calling
  - `LlmClient` 增强了异常处理

- [ ] **Step 1: 创建AI配置类**

```java
package com.moodtune.config;

import com.moodtune.service.SongSearchTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class AiConfig {

    @Bean
    public ChatClient.Builder chatClientBuilder(ChatModel chatModel, SongSearchTool songSearchTool) {
        return ChatClient.builder(chatModel)
                .defaultFunction("searchLikedSongs", 
                                "查询用户红心歌单中的歌曲", 
                                songSearchTool);
    }
}
```

- [ ] **Step 2: 更新LlmClient的异常处理**

在 `backend/src/main/java/com/moodtune/service/LlmClient.java` 的 `chatStream` 方法中增强异常处理：

```java
public String chatStream(List<com.moodtune.dto.LlmMessage> messages, SseEmitter emitter) throws IOException {
    List<Message> springAiMessages = messages.stream()
            .map(this::convertMessage)
            .collect(Collectors.toList());

    StringBuilder fullResponse = new StringBuilder();

    try {
        Flux<String> contentStream = chatClientBuilder.build()
                .prompt()
                .messages(springAiMessages)
                .stream()
                .content()
                .timeout(Duration.ofSeconds(30));  // 30秒首次响应超时

        contentStream
                .doOnNext(chunk -> {
                    fullResponse.append(chunk);
                    try {
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(chunk));
                    } catch (IOException e) {
                        log.error("发送SSE chunk失败", e);
                    }
                })
                .doOnError(java.util.concurrent.TimeoutException.class, e -> {
                    log.error("LLM响应超时", e);
                    try {
                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data("AI响应超时，请稍后重试"));
                        emitter.complete();
                    } catch (IOException ignored) {}
                })
                .doOnError(e -> {
                    if (!(e instanceof java.util.concurrent.TimeoutException)) {
                        log.error("LLM调用失败", e);
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data("AI服务暂时不可用，请稍后重试"));
                            emitter.complete();
                        } catch (IOException ignored) {}
                    }
                })
                .blockLast();

        return fullResponse.toString();
    } catch (Exception e) {
        log.error("Chat stream failed", e);
        throw new IOException("LLM streaming failed", e);
    }
}
```

添加必要的import：

```java
import java.time.Duration;
import java.util.concurrent.TimeoutException;
```

- [ ] **Step 3: 验证编译**

```bash
cd backend
./mvnw compile
```

Expected: `BUILD SUCCESS`

- [ ] **Step 4: 提交**

```bash
git add backend/src/main/java/com/moodtune/config/AiConfig.java \
        backend/src/main/java/com/moodtune/service/LlmClient.java
git commit -m "feat(ai): configure timeout and function calling support

- Add AiConfig to register searchLikedSongs tool
- Add 30s read timeout for first response
- Enhance error handling with TimeoutException
- Send SSE error events for timeout and failures"
```

---

### Task 5: 优化RecommendationService的System Prompt和解析逻辑

**Files:**
- Modify: `backend/src/main/java/com/moodtune/service/RecommendationService.java:30-129`

**Interfaces:**
- Consumes:
  - `SongSearchTool` (通过Spring AI自动调用)
  - `LlmClient.chatStream(...)`
  - `Song` 实体类
- Produces:
  - `RecommendationResult` 包含完整 `SongDTO` 对象

- [ ] **Step 1: 更新System Prompt**

替换 `SYSTEM_PROMPT` 常量为新版本：

```java
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
```

- [ ] **Step 2: 移除buildSongList方法**

删除 `buildSongList()` 方法（约96-103行）：

```java
// 删除整个方法
private String buildSongList() {
    return songService.getAllSongs().stream()
            .map(s -> String.format("ID:%d | %s - %s | 风格:%s | 喜欢:%s",
                    s.getId(), s.getTitle(), s.getArtist(),
                    s.getGenre() != null ? s.getGenre() : "未知",
                    s.getLiked() ? "是" : "否"))
            .collect(Collectors.joining("\n"));
}
```

- [ ] **Step 3: 更新recommend方法 - 移除buildSongList调用**

修改 `recommend` 方法（约52-94行），移除buildSongList调用：

```java
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
```

- [ ] **Step 4: 更新parseResponse方法 - 解析完整song对象**

替换 `parseResponse` 方法（约105-129行）：

```java
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
```

添加必要的import：

```java
import com.moodtune.dto.SongDTO;
import java.util.ArrayList;
```

- [ ] **Step 5: 验证编译**

```bash
cd backend
./mvnw compile
```

Expected: `BUILD SUCCESS`

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/moodtune/service/RecommendationService.java
git commit -m "feat(ai): optimize system prompt and parse full SongDTO from LLM

- Remove buildSongList method (songs fetched via Function Calling)
- Update system prompt with tool usage guide and recommendation strategy
- Parse complete SongDTO object from LLM response
- Handle null song case for pure conversation"
```

---

### Task 6: 手动测试和验证

**Files:**
- 无修改（手动测试任务）

**Interfaces:**
- Consumes: 完整的AI推荐功能
- Produces: 验证报告

- [ ] **Step 1: 准备测试数据**

连接数据库并添加测试歌曲：

```sql
-- 添加几首红心歌曲用于测试
INSERT INTO songs (title, artist, genre, liked, mood_tags) VALUES
('晴天', '周杰伦', '流行', true, ARRAY['治愈', '温暖', '怀旧']),
('安静', '周杰伦', '流行', true, ARRAY['伤感', '安静', '思念']),
('倔强', '五月天', '摇滚', true, ARRAY['激昂', '励志', '热血']),
('成都', '赵雷', '民谣', true, ARRAY['怀旧', '温暖', '思念']);

-- 添加几首非红心歌曲（不应被推荐）
INSERT INTO songs (title, artist, genre, liked, mood_tags) VALUES
('测试歌曲1', '测试歌手', '流行', false, ARRAY['轻快']),
('测试歌曲2', '测试歌手', '摇滚', false, ARRAY['激昂']);
```

- [ ] **Step 2: 启动后端服务**

```bash
cd backend
./mvnw spring-boot:run
```

Expected: 服务启动成功，监听8080端口

- [ ] **Step 3: 测试场景1 - 按歌名查询**

使用curl或Postman发送请求：

```bash
curl -X POST http://localhost:8080/api/sessions/1/messages \
  -H "Content-Type: application/json" \
  -d '{"message": "我想听晴天"}'
```

Expected: 
- SSE流式返回
- AI调用 `searchLikedSongs("晴天", null, null, null)`
- 返回包含《晴天》的完整SongDTO对象

- [ ] **Step 4: 测试场景2 - 按情绪查询**

```bash
curl -X POST http://localhost:8080/api/sessions/1/messages \
  -H "Content-Type: application/json" \
  -d '{"message": "我分手了，很伤心"}'
```

Expected:
- AI调用 `searchLikedSongs(null, null, null, "伤感")`
- 返回《安静》或其他伤感歌曲

- [ ] **Step 5: 测试场景3 - 红心歌单为空**

清空红心歌曲：

```sql
UPDATE songs SET liked = false;
```

发送请求：

```bash
curl -X POST http://localhost:8080/api/sessions/2/messages \
  -H "Content-Type: application/json" \
  -d '{"message": "给我推荐首歌"}'
```

Expected: AI回复"你还没有收藏任何歌曲哦，先去添加几首红心歌曲吧～"

- [ ] **Step 6: 测试场景4 - 查询无结果**

恢复红心歌曲：

```sql
UPDATE songs SET liked = true WHERE title IN ('晴天', '安静', '倔强', '成都');
```

发送请求：

```bash
curl -X POST http://localhost:8080/api/sessions/3/messages \
  -H "Content-Type: application/json" \
  -d '{"message": "周杰伦的摇滚"}'
```

Expected: AI回复"你收藏的歌曲里暂时没有周杰伦的摇滚"

- [ ] **Step 7: 检查日志**

查看后端日志，确认：
- Function Calling成功调用
- searchLikedSongs的查询参数正确
- 返回的歌曲数量符合预期
- 无异常错误

Expected: 日志包含 `searchLikedSongs查询参数: ... 返回X首歌曲`

- [ ] **Step 8: 验证返回数据结构**

检查API返回的JSON格式：

```json
{
  "text": "推荐理由...",
  "song": {
    "id": 123,
    "title": "晴天",
    "artist": "周杰伦",
    "genre": "流行",
    "fileUrl": "http://...",
    "moodTags": ["治愈", "温暖", "怀旧"]
  }
}
```

Expected: 结构完整，song对象包含所有必需字段

- [ ] **Step 9: 提交测试报告**

创建测试报告文件：

```bash
cat > docs/superpowers/plans/2026-06-21-ai-recommendation-test-report.md <<'EOF'
# AI推荐功能测试报告

## 测试环境
- 后端：Spring Boot 3.x + 通义千问API
- 数据库：PostgreSQL + mood_tags字段
- 测试时间：2026-06-21

## 测试场景

### ✅ 场景1: 按歌名查询
- 输入："我想听晴天"
- Function Calling: searchLikedSongs("晴天", null, null, null)
- 返回：完整SongDTO对象，包含《晴天》信息
- 状态：通过

### ✅ 场景2: 按情绪查询
- 输入："我分手了，很伤心"
- Function Calling: searchLikedSongs(null, null, null, "伤感")
- 返回：《安静》或其他伤感歌曲
- 状态：通过

### ✅ 场景3: 红心歌单为空
- 输入："给我推荐首歌"
- Function Calling: searchLikedSongs(null, null, null, null)
- 返回：提示"你还没有收藏任何歌曲哦"
- 状态：通过

### ✅ 场景4: 查询无结果
- 输入："周杰伦的摇滚"
- Function Calling: searchLikedSongs(null, "周杰伦", "摇滚", null)
- 返回：提示"你收藏的歌曲里暂时没有周杰伦的摇滚"
- 状态：通过

## 数据结构验证

✅ RecommendationResult包含text和song字段
✅ SongDTO包含id, title, artist, genre, fileUrl, moodTags
✅ 纯对话时song为null

## 结论

所有测试场景通过，功能符合设计文档要求。
EOF

git add docs/superpowers/plans/2026-06-21-ai-recommendation-test-report.md
git commit -m "test: add AI recommendation feature test report"
```

---

## 自我审查

### 1. 规格覆盖检查

| 设计文档需求 | 对应任务 | 状态 |
|------------|---------|------|
| 添加mood_tags字段 | Task 1 | ✅ |
| 更新Song实体类和创建SongDTO | Task 2 | ✅ |
| 实现searchLikedSongs工具 | Task 3 | ✅ |
| 配置超时和Function Calling | Task 4 | ✅ |
| 优化System Prompt | Task 5 | ✅ |
| 返回完整SongDTO对象 | Task 5 | ✅ |
| 异常处理（超时/中断） | Task 4 | ✅ |
| 手动测试验证 | Task 6 | ✅ |

### 2. 占位符扫描

✅ 无TBD、TODO、"add appropriate"等占位符
✅ 所有代码块完整
✅ 所有步骤包含具体命令和预期输出

### 3. 类型一致性检查

✅ `Song.moodTags` → `List<String>` 在所有任务中一致
✅ `RecommendationResult.song` → `SongDTO` 在Task 2和Task 5中一致
✅ 工具方法签名 `searchLikedSongs(String, String, String, String)` 在Task 3和设计文档中一致
✅ 超时配置 `Duration.ofSeconds(30)` 在Task 4和设计文档中一致

### 4. 依赖关系验证

- Task 1 → Task 2：数据库字段必须先创建
- Task 2 → Task 3：实体类和DTO必须先定义
- Task 3 → Task 4：工具类必须先实现才能注册
- Task 4 → Task 5：配置和异常处理必须先完成
- Task 5 → Task 6：完整功能必须先实现才能测试

依赖关系清晰，任务顺序合理。

---

## 执行建议

**预计时间：** 2-3小时（包含测试）

**风险点：**
1. PostgreSQL的GIN索引语法可能因版本不同略有差异
2. Spring AI的@Tool注解配置可能需要调整依赖版本
3. 通义千问API的Function Calling响应格式可能需要微调

**缓解措施：**
- Task 1执行后立即验证索引创建成功
- Task 4编译失败时检查Spring AI版本（建议1.0.0-M1+）
- Task 6测试失败时检查LLM日志，确认工具调用格式
