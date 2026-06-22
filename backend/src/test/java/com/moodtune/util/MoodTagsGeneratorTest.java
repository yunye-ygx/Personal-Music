package com.moodtune.util;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.moodtune.entity.Song;
import com.moodtune.mapper.SongMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

/**
 * 为所有歌曲生成mood_tags的工具测试
 * 运行方式：右键点击这个测试类 -> Run 'MoodTagsGeneratorTest'
 */
@Slf4j
@SpringBootTest
public class MoodTagsGeneratorTest {

    @Autowired
    private SongMapper songMapper;

    @Autowired
    @Qualifier("customChatClientBuilder")
    private ChatClient.Builder chatClientBuilder;

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

    @Test
    public void generateMoodTagsForAllSongs() throws Exception {
        log.info("========================================");
        log.info("开始为所有歌曲生成mood_tags...");
        log.info("========================================");

        // 查询所有mood_tags为空的歌曲
        QueryWrapper<Song> wrapper = new QueryWrapper<>();
        wrapper.isNull("mood_tags").or().apply("array_length(mood_tags, 1) IS NULL");
        List<Song> songs = songMapper.selectList(wrapper);

        if (songs.isEmpty()) {
            log.info("所有歌曲已有mood_tags，无需生成");
            return;
        }

        log.info("找到{}首歌曲需要生成mood_tags\n", songs.size());

        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < songs.size(); i++) {
            Song song = songs.get(i);
            try {
                log.info("[{}/{}] 处理: {} - {}",
                         i + 1, songs.size(), song.getTitle(), song.getArtist());

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
                    log.warn("  ⚠️  LLM返回为空，使用默认标签");
                    moodTags = List.of("未分类");
                }

                // 更新数据库
                song.setMoodTags(moodTags);
                songMapper.updateById(song);

                log.info("  ✅ 生成标签: {}\n", moodTags);
                successCount++;

                // 避免频繁调用API
                Thread.sleep(500);

            } catch (Exception e) {
                log.error("  ❌ 处理失败: {}\n", e.getMessage());
                failCount++;
            }
        }

        log.info("========================================");
        log.info("mood_tags生成完成！");
        log.info("成功: {} 首", successCount);
        log.info("失败: {} 首", failCount);
        log.info("========================================");
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
