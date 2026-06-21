package com.moodtune.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmClient {

    private final ChatClient.Builder chatClientBuilder;

    /**
     * 流式调用 - 实时推送每个生成的token到SseEmitter
     * 返回完整累积的响应文本
     */
    public String chatStream(List<com.moodtune.dto.LlmMessage> messages, SseEmitter emitter) throws IOException {
        // 转换消息格式
        List<Message> springAiMessages = messages.stream()
                .map(this::convertMessage)
                .collect(Collectors.toList());

        StringBuilder fullResponse = new StringBuilder();

        try {
            // 使用Spring AI的流式API
            Flux<String> contentStream = chatClientBuilder.build()
                    .prompt()
                    .messages(springAiMessages)
                    .stream()
                    .content();

            // 订阅流式响应
            contentStream
                    .doOnNext(chunk -> {
                        fullResponse.append(chunk);
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("message")
                                    .data(chunk));
                        } catch (IOException e) {
                            log.error("Failed to send SSE chunk", e);
                        }
                    })
                    .doOnError(e -> {
                        log.error("Stream error", e);
                        emitter.completeWithError(e);
                    })
                    .blockLast(); // 阻塞等待流完成

            return fullResponse.toString();
        } catch (Exception e) {
            log.error("Chat stream failed", e);
            throw new IOException("LLM streaming failed", e);
        }
    }

    /**
     * 非流式调用 - 等待完整响应
     */
    public String chat(List<com.moodtune.dto.LlmMessage> messages) {
        List<Message> springAiMessages = messages.stream()
                .map(this::convertMessage)
                .collect(Collectors.toList());

        try {
            return chatClientBuilder.build()
                    .prompt()
                    .messages(springAiMessages)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Chat call failed", e);
            return "";
        }
    }

    /**
     * 转换自定义消息格式到Spring AI消息格式
     */
    private Message convertMessage(com.moodtune.dto.LlmMessage msg) {
        return switch (msg.getRole().toLowerCase()) {
            case "system" -> new SystemMessage(msg.getContent());
            case "user" -> new UserMessage(msg.getContent());
            case "assistant" -> new AssistantMessage(msg.getContent());
            default -> throw new IllegalArgumentException("Unknown role: " + msg.getRole());
        };
    }
}
