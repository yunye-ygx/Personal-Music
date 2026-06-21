# AI推荐功能优化设计文档

## 1. 设计目标

优化现有的AI音乐推荐功能，使其能够：
- 严格从用户红心歌曲中推荐
- 支持基于情绪、风格、歌手等多维度的智能查询
- 通过Function Calling按需查询，提升可扩展性
- 返回完整歌曲信息，前端直接可播放
- 完善异常处理，提升用户体验

## 2. 当前实现的问题

1. **推荐范围不精确**：从所有歌曲中推荐，未利用"红心"标记
2. **可扩展性差**：在system prompt中列出所有歌曲，token消耗大
3. **前端请求冗余**：只返回song_id，前端需要额外查询歌曲详情
4. **缺少情绪维度**：无法基于用户心情进行精准推荐
5. **异常处理不完善**：缺少超时、中断等场景的处理

## 3. 数据库变更

### 3.1 增加情绪标签字段

```sql
ALTER TABLE songs ADD COLUMN mood_tags TEXT[];
```

**字段说明：**
- 类型：TEXT数组
- 用途：存储歌曲的情绪标签，支持多标签
- 示例：`['治愈', '温暖', '怀旧']`

**常见情绪标签：**
- 正向：治愈、温暖、轻快、振奋、励志、热血
- 负向：伤感、忧郁、思念、孤独
- 中性：安静、舒缓、平静、怀旧

### 3.2 索引优化

```sql
CREATE INDEX idx_songs_mood_tags ON songs USING GIN(mood_tags);
```

GIN索引支持数组类型的高效查询。

### 3.3 未来扩展计划

当需要添加歌词、封面等字段时，拆分为两表：

```sql
-- 核心表（高频查询）
CREATE TABLE songs (
    id, title, artist, genre, mood_tags, 
    file_url, liked, created_at
);

-- 详情表（低频查询）
CREATE TABLE song_details (
    song_id BIGINT PRIMARY KEY REFERENCES songs(id),
    lyrics TEXT,
    cover_url VARCHAR(500),
    album VARCHAR(255),
    release_year INT,
    duration INT,
    tempo VARCHAR(50),
    energy VARCHAR(50)
);
```

## 4. Function Calling工具设计

### 4.1 工具定义

```java
@Tool(description = "查询用户红心歌单中的歌曲。所有参数都是可选的，如果都不提供则返回所有红心歌曲。")
public List<Song> searchLikedSongs(
    @Param(description = "歌曲名称。用于精确查询歌名。例如：晴天、七里香") 
    String title,
    
    @Param(description = "歌手名称。例如：周杰伦、陈奕迅") 
    String artist,
    
    @Param(description = "音乐风格。例如：民谣、摇滚、流行、爵士") 
    String genre,
    
    @Param(description = "情绪或关键词。会在歌名、歌手、风格、情绪标签中模糊搜索。例如：安静、伤感、治愈、激昂") 
    String keyword
)
```

### 4.2 查询逻辑

```sql
SELECT * FROM songs 
WHERE liked = true 
  AND (title ILIKE CONCAT('%', :title, '%') OR :title IS NULL)
  AND (artist ILIKE CONCAT('%', :artist, '%') OR :artist IS NULL)
  AND (genre = :genre OR :genre IS NULL)
  AND (
    :keyword IS NULL OR
    title ILIKE CONCAT('%', :keyword, '%') OR
    artist ILIKE CONCAT('%', :keyword, '%') OR
    genre ILIKE CONCAT('%', :keyword, '%') OR
    :keyword = ANY(mood_tags)
  )
LIMIT 20;
```

**查询规则：**
1. **强制条件**：`liked = true`（只查红心歌曲）
2. **可选精确匹配**：title、artist、genre
3. **可选模糊匹配**：keyword在所有文本字段中搜索
4. **限制返回数量**：最多20首，避免结果过多

### 4.3 工具调用示例

| 用户输入 | AI调用 | 返回结果 |
|---------|--------|---------|
| "我想听晴天" | `searchLikedSongs("晴天", null, null, null)` | 返回歌名包含"晴天"的红心歌曲 |
| "周杰伦的民谣" | `searchLikedSongs(null, "周杰伦", "民谣", null)` | 返回周杰伦的民谣风格红心歌曲 |
| "我分手了很伤心" | `searchLikedSongs(null, null, null, "伤感")` | 返回mood_tags包含"伤感"的歌曲 |
| "给我推荐首歌" | `searchLikedSongs(null, null, null, null)` | 返回所有红心歌曲（最多20首） |
| "安静的歌" | `searchLikedSongs(null, null, null, "安静")` | 在所有字段中搜索"安静" |

### 4.4 工具异常处理

```java
@Tool
public List<Song> searchLikedSongs(String title, String artist, String genre, String keyword) {
    try {
        // 参数校验：至少一个参数有值或全为空（查所有）
        QueryWrapper<Song> wrapper = new QueryWrapper<>();
        wrapper.eq("liked", true);
        
        if (title != null) {
            wrapper.like("title", title);
        }
        if (artist != null) {
            wrapper.like("artist", artist);
        }
        if (genre != null) {
            wrapper.eq("genre", genre);
        }
        if (keyword != null) {
            wrapper.and(w -> w
                .like("title", keyword)
                .or().like("artist", keyword)
                .or().like("genre", keyword)
                .or().apply("? = ANY(mood_tags)", keyword)
            );
        }
        
        wrapper.last("LIMIT 20");
        
        List<Song> results = songMapper.selectList(wrapper);
        log.info("searchLikedSongs返回{}首歌曲", results.size());
        return results;
        
    } catch (Exception e) {
        log.error("查询红心歌曲失败", e);
        // Spring AI会将异常信息传给LLM
        throw new RuntimeException("歌曲查询失败，请稍后重试");
    }
}
```

**异常场景：**
1. **数据库连接失败**：抛出异常，LLM收到错误信息后告知用户
2. **返回空列表**：不抛异常，LLM根据空列表生成"没有找到这类歌"的回复

## 5. AI返回数据结构

### 5.1 新结构定义

```java
@Data
public class RecommendationResult {
    private String text;        // AI的回复文字
    private SongDTO song;       // 推荐的歌曲对象（可为null）
}

@Data
public class SongDTO {
    private Long id;
    private String title;
    private String artist;
    private String genre;
    private String fileUrl;
    private List<String> moodTags;
}
```

### 5.2 返回示例

**推荐歌曲：**
```json
{
  "text": "分手总是让人难过的，这首《安静》或许能陪你度过这段时光",
  "song": {
    "id": 123,
    "title": "安静",
    "artist": "周杰伦",
    "genre": "流行",
    "fileUrl": "http://minio:9000/moodtune-songs/anjing.mp3",
    "moodTags": ["伤感", "安静", "思念"]
  }
}
```

**纯对话（不推荐歌曲）：**
```json
{
  "text": "你想听什么风格的歌呢？民谣、摇滚还是流行？",
  "song": null
}
```

### 5.3 前端使用

前端收到后直接渲染：
```javascript
if (result.song) {
  // 渲染歌曲卡片
  const card = {
    title: result.song.title,
    artist: result.song.artist,
    playUrl: result.song.fileUrl,  // 点击直接播放
    tags: result.song.moodTags
  };
}
```

## 6. System Prompt优化

### 6.1 旧版Prompt（直接列出所有歌曲）

```
你是一个私人音乐顾问，是用户的知心朋友。
你的任务是根据用户当下的状态和心情，从他们的歌单中推荐一首最合适的歌。

你的歌单如下：
ID:1 | 晴天 - 周杰伦 | 风格:流行 | 喜欢:是
ID:2 | 安静 - 周杰伦 | 风格:流行 | 喜欢:否
ID:3 | 倔强 - 五月天 | 风格:摇滚 | 喜欢:是
...（可能有几百首）

当前时间：2026-06-21 14:30 星期六
```

**问题：**
- 所有歌曲都在prompt中，token消耗大
- 扩展性差（歌曲多了无法全部列出）
- 未区分红心和非红心歌曲

### 6.2 新版Prompt（使用Function Calling）

```
你是一个私人音乐顾问，是用户的知心朋友。
你的任务是根据用户当下的状态和心情，从他们的红心歌单中推荐歌曲。

## 工具使用指南

使用 searchLikedSongs 工具查询用户的红心歌曲：
- 根据用户的描述提取关键信息（歌名、歌手、风格、情绪）
- 调用工具时，提供尽可能明确的参数
- 从查询结果中选择最合适的一首歌

## 推荐策略

1. **理解真实需求**：用户说"伤心"时，可能想要共鸣的歌，也可能想要振奋的歌。根据上下文判断。
2. **情绪匹配优先**：优先考虑歌曲的情绪基调（通过mood_tags和歌词主题）
3. **避免重复推荐**：如果用户说"换一首"，避免推荐聊天历史中已推荐的歌
4. **空结果处理**：如果工具返回空列表，告诉用户"你的红心歌单里暂时没有这类歌曲"
5. **引导明确需求**：如果用户需求模糊，可以问"你是想找首歌陪你难过，还是想听点振奋的？"

## 特殊场景

- 用户说"给我推荐首歌"且没有更多上下文：调用 searchLikedSongs() 不传任何参数，从所有红心歌曲中随机选一首
- 红心歌单为空（工具返回空列表且用户是第一次询问）：提示"你还没有收藏任何歌曲哦，先去添加几首红心歌曲吧～"
- 工具调用失败（抛出异常）：告知用户"抱歉，查询歌曲时出了点问题，请稍后再试"

当前时间：{currentTime}

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
```

### 6.3 Prompt对比

| 维度 | 旧版 | 新版 |
|------|------|------|
| Token消耗 | 高（列出所有歌曲） | 低（按需查询） |
| 可扩展性 | 差（歌曲多了放不下） | 好（工具调用） |
| 红心精准度 | 差（包含非红心歌曲） | 好（强制liked=true） |
| 情绪维度 | 无 | 有（mood_tags） |

## 7. 异常处理策略

### 7.1 异常场景分类

| 异常场景 | 触发条件 | 处理方式 | 前端显示 |
|---------|---------|---------|---------|
| **红心歌单为空** | searchLikedSongs返回空列表，且是首次对话 | AI自然回复 | "你还没有收藏任何歌曲哦，先去添加几首红心歌曲吧～" |
| **查询无结果** | searchLikedSongs返回空列表 | AI告知没有匹配 | "你收藏的歌曲里暂时没有周杰伦的摇滚，要不要试试别的？" |
| **工具调用失败** | searchLikedSongs抛出异常（数据库故障） | Spring AI传递异常给LLM | "抱歉，查询歌曲时出了点问题，请稍后再试" |
| **LLM连接超时** | 10秒内无法连接到API | 捕获TimeoutException | "AI服务连接超时，请检查网络" + 重试按钮 |
| **LLM首次响应超时** | 30秒内没有返回第一个chunk | 捕获ReadTimeoutException | "AI响应超时，请稍后重试" + 重试按钮 |
| **流式输出中断** | 已返回部分chunk后连接中断 | 保留已生成部分，发送error事件 | 显示已生成文字 + "⚠️ 生成中断" + 重试按钮 |
| **LLM返回格式错误** | 无法解析为JSON | 降级为纯文本回复 | 显示文字，不显示歌曲卡片 |

### 7.2 超时配置

```java
@Bean
public ChatClient.Builder chatClientBuilder(ChatModel chatModel) {
    return ChatClient.builder(chatModel)
        .defaultOptions(ChatOptions.builder()
            .connectTimeout(Duration.ofSeconds(10))   // 连接超时：10秒
            .readTimeout(Duration.ofSeconds(30))      // 首次响应超时：30秒
            .build());
}
```

**超时说明：**
- `connectTimeout`：建立TCP连接的时限
- `readTimeout`：**开始返回第一个数据块**的时限，不是整个任务的时限
- 一旦开始流式返回，只要持续有数据，可以运行更长时间（例如总共50秒）

### 7.3 SSE事件类型设计

```java
// 正常数据chunk
emitter.send(SseEmitter.event()
    .name("message")
    .data(chunk));

// 生成完成
emitter.send(SseEmitter.event()
    .name("done")
    .data(""));

// 错误（中断或异常）
emitter.send(SseEmitter.event()
    .name("error")
    .data(Map.of(
        "message", "AI响应超时",
        "code", "TIMEOUT"
    )));
```

**前端处理示例：**
```javascript
const eventSource = new EventSource('/api/sessions/1/messages');
let accumulatedText = '';

eventSource.addEventListener('message', (e) => {
    accumulatedText += e.data;
    updateUI(accumulatedText);
});

eventSource.addEventListener('done', (e) => {
    // 解析最终的JSON，提取song对象
    const result = JSON.parse(accumulatedText);
    renderSongCard(result.song);
});

eventSource.addEventListener('error', (e) => {
    // 保留已显示的文字，显示错误提示和重试按钮
    showErrorBanner(e.data);
    eventSource.close();
});
```

## 8. 实现路径

### 8.1 数据库迁移
1. 添加 `mood_tags` 字段到 `songs` 表
2. 创建GIN索引
3. 为现有歌曲补充mood_tags（可手动或AI生成）

### 8.2 后端改造
1. 创建 `SongDTO` 类（包含moodTags）
2. 实现 `searchLikedSongs` 工具函数（使用@Tool注解）
3. 修改 `RecommendationResult` 结构（从song_id改为song对象）
4. 优化 `RecommendationService`：
   - 移除buildSongList方法
   - 更新system prompt
   - 改进parseResponse解析逻辑
5. 完善 `LlmClient` 的异常处理和超时配置
6. 更新 `ChatController` 的SSE事件类型

### 8.3 前端适配
1. 更新消息渲染逻辑（接收完整song对象）
2. 实现歌曲卡片点击播放功能
3. 完善error事件处理和重试UI
4. 添加"生成中断"提示

### 8.4 测试场景
1. 正常推荐流程
2. 红心歌单为空
3. 查询无结果
4. 网络超时
5. 流式输出中断
6. 工具调用失败

## 9. 后续优化方向

1. **情绪标签智能生成**：导入歌曲时，调用LLM自动生成mood_tags
2. **推荐多样性**：避免总是推荐同一首歌，增加随机性
3. **上下文记忆**：记录用户的推荐反馈（跳过/喜欢），优化后续推荐
4. **歌曲详情表拆分**：当需要歌词、封面等扩展字段时进行拆分
5. **向量检索**：使用pgvector实现基于语义的歌曲搜索
