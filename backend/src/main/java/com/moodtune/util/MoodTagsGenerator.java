package com.moodtune.util;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moodtune.entity.Song;
import com.moodtune.mapper.SongMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 临时工具：为所有歌曲自动生成mood_tags
 * 使用后可以删除此类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MoodTagsGenerator implements CommandLineRunner {

    private final SongMapper songMapper;
    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;

    private static final String MOOD_TAGS_PROMPT = """
            你是一个音乐情绪分析专家。根据歌曲名称和歌手，生成3个最符合这首歌情绪特征的标签。

            可选标签范围：
            - 正向：治愈、温暖、轻快、振奋、励志、热血、欢快、浪漫
            - 负向：伤感、忧郁、思念、孤独、悲伤、失落
            - 中性：安静、舒缓、平静、怀旧、深情、温柔

            歌曲信息：
            歌名：%s
            歌手：%s

            要求：
            1. 只返回3个标签，用逗号分隔
            2. 标签必须从上述范围中选择
            3. 直接返回标签，不要其他解释

            示例格式：治愈,温暖,怀旧
            """;

    @Override
    public void run(String... args) throws Exception {
        // 检查是否需要运行（通过环境变量或启动参数控制）
        String enableGenerator = System.getProperty("moodtune.generate-tags", "false");
        if (!"true".equalsIgnoreCase(enableGenerator)) {
            log.info("MoodTagsGenerator未启用。如需生成标签，请添加启动参数：-Dmoodtune.generate-tags=true");
            return;
        }

        log.info("开始为所有歌曲生成mood_tags...");

        // 查询所有mood_tags为空的歌曲
        QueryWrapper<Song> wrapper = new QueryWrapper<>();
        wrapper.isNull("mood_tags").or().apply("array_length(mood_tags, 1) IS NULL");
        List<Song> songs = songMapper.selectList(wrapper);

        if (songs.isEmpty()) {
            log.info("所有歌曲已有mood_tags，无需生成");
            return;
        }

        log.info("找到{}首歌曲需要生成mood_tags", songs.size());

        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < songs.size(); i++) {
            Song song = songs.get(i);
            try {
                log.info("处理 [{}/{}]: {} - {}", i + 1, songs.size(), song.getTitle(), song.getArtist());

                // 调用LLM生成标签
                String prompt = String.format(MOOD_TAGS_PROMPT, song.getTitle(), song.getArtist());
                String response = chatClientBuilder.build()
                        .prompt()
                        .user(prompt)
                        .call()
                        .content();

                // 解析标签
                List<String> moodTags = parseMoodTags(response.trim());

                if (moodTags.isEmpty()) {
                    log.warn("  ⚠️ LLM返回为空，使用默认标签");
                    moodTags = List.of("未分类");
                }

                // 更新数据库
                song.setMoodTags(moodTags);
                songMapper.updateById(song);

                log.info("  ✅ 生成标签: {}", moodTags);
                successCount++;

                // 避免频繁调用API，添加短暂延迟
                if (i < songs.size() - 1) {
                    Thread.sleep(500);
                }

            } catch (Exception e) {
                log.error("  ❌ 处理失败: {}", e.getMessage());
                failCount++;
            }
        }

        log.info("========================================");
        log.info("mood_tags生成完成！");
        log.info("成功: {} 首", successCount);
        log.info("失败: {} 首", failCount);
        log.info("========================================");
        log.info("提示：可以删除 MoodTagsGenerator.java 文件了");
    }

    /**
     * 解析LLM返回的标签文本
     */
    private List<String> parseMoodTags(String response) {
        List<String> tags = new ArrayList<>();

        // 移除可能的markdown格式
        response = response.replaceAll("```.*", "").trim();

        // 按逗号分割
        String[] parts = response.split("[,，]");
        for (String part : parts) {
            String tag = part.trim();
            if (!tag.isEmpty()) {
                tags.add(tag);
            }
        }

        return tags;
    }
}
