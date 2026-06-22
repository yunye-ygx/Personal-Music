package com.moodtune.config;

import com.moodtune.service.SongSearchTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public ChatClient.Builder customChatClientBuilder(ChatModel chatModel, SongSearchTool songSearchTool) {
        return ChatClient.builder(chatModel)
                .defaultFunction("searchLikedSongs",
                                "查询用户红心歌单中的歌曲",
                                songSearchTool);
    }
}
