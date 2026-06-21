package com.moodtune.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * 音乐风格识别服务
 * 使用 Spring AI + 通义千问根据歌名和歌手自动识别音乐风格
 */
@Slf4j
@Service
public class MusicGenreService {

    private final ChatClient chatClient;

    public MusicGenreService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    /**
     * 根据歌名和歌手预测音乐风格
     */
    public String predictGenre(String title, String artist) {
        try {
            String prompt = String.format(
                    "根据以下歌曲信息判断音乐风格，只返回一个简短的中文风格标签（如：流行、摇滚、电音、民谣、古风、R&B、嘻哈、爵士等），不要返回任何解释：\n" +
                    "歌名：%s\n" +
                    "歌手：%s\n" +
                    "风格：",
                    title, artist
            );

            log.info("调用 Spring AI 识别音乐风格 - 歌名: {}, 歌手: {}", title, artist);

            String genre = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content()
                    .trim();

            log.info("AI 识别结果: {}", genre);

            // 如果 AI 返回空或太长，使用默认值
            if (genre.isEmpty() || genre.length() > 20) {
                log.warn("AI 返回结果异常，使用默认风格");
                return "流行";
            }

            return genre;

        } catch (Exception e) {
            log.error("Spring AI 识别音乐风格失败", e);
            return "流行";
        }
    }
}
