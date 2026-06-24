package com.moodtune.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moodtune.tool.SearchLikedSongsCallback;
import com.moodtune.tool.SongSearchTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AiConfig {

    @Bean
    public SearchLikedSongsCallback searchLikedSongsCallback(SongSearchTool songSearchTool,
                                                             ObjectMapper objectMapper) {
        return new SearchLikedSongsCallback(songSearchTool, objectMapper);
    }

    @Bean
    @Primary
    public ChatClient.Builder customChatClientBuilder(ChatModel chatModel,
                                                      SearchLikedSongsCallback searchLikedSongsCallback) {
        return ChatClient.builder(chatModel)
                .defaultToolCallbacks(searchLikedSongsCallback);
    }
}
