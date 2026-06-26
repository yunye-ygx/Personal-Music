package com.moodtune.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmClient {

    private final ChatClient.Builder customChatClientBuilder;

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
            // 使用Spring AI的流式API，设置30秒首次响应超时
            Flux<String> contentStream = customChatClientBuilder.build()
                    .prompt()
                    .messages(springAiMessages)
                    .stream()
                    .content()
                    .timeout(Duration.ofSeconds(30));  // 30秒首次响应超时

            // 订阅流式响应
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
                    .doOnError(TimeoutException.class, e -> {
                        log.error("LLM响应超时", e);
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data("AI响应超时，请稍后重试"));
                            emitter.complete();
                        } catch (IOException ignored) {}
                    })
                    .doOnError(e -> {
                        if (!(e instanceof TimeoutException)) {
                            log.error("LLM调用失败", e);
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data("AI服务暂时不可用，请稍后重试"));
                                emitter.complete();
                            } catch (IOException ignored) {}
                        }
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
            String content = customChatClientBuilder.build()
                    .prompt()
                    .messages(springAiMessages)
                    .call()
                    .content();
            log.info("Chat response length: {}", content != null ? content.length() : 0);
            if (content != null && !content.isBlank()) {
                log.debug("Chat response: {}", content);
            }
            return content != null ? content : "";
        } catch (Exception e) {
            log.error("Chat call failed", e);
            return "";
        }
    }

    /**
     * 流式调用 + 工具回调 - 支持 Function Calling 的流式响应
     */
    public String chatStreamWithTools(List<com.moodtune.dto.LlmMessage> messages,
                                      SseEmitter emitter,
                                      List<ToolCallback> toolCallbacks) throws IOException {
        List<Message> springAiMessages = messages.stream()
                .map(this::convertMessage)
                .collect(Collectors.toList());

        StringBuilder fullResponse = new StringBuilder();

        try {
            var client = customChatClientBuilder.build();
            var promptBuilder = client.prompt().messages(springAiMessages);
            if (toolCallbacks != null && !toolCallbacks.isEmpty()) {
                promptBuilder.toolCallbacks(toolCallbacks);
            }

            Flux<String> contentStream = promptBuilder
                    .stream()
                    .content()
                    .timeout(Duration.ofSeconds(30));

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
                    .doOnError(TimeoutException.class, e -> {
                        log.error("LLM响应超时", e);
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data("AI响应超时，请稍后重试"));
                            emitter.complete();
                        } catch (IOException ignored) {}
                    })
                    .doOnError(e -> {
                        if (!(e instanceof TimeoutException)) {
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
            log.error("Chat stream with tools failed", e);
            throw new IOException("LLM streaming failed", e);
        }
    }

    /**
     * 流式调用 + 工具回调 + JSON text 字段实时提取
     *
     * LLM 输出格式：{"text":"自然语言回复","song":{"title":"...","artist":"..."}}
     *
     * 处理逻辑：
     * - 实时扫描流式 buffer，找到 "text":" 后开始把内容推给前端（event: message）
     * - 遇到 text 字段结束（",  或 "} ）时停止推送
     * - 流结束后返回完整原始 JSON 供调用方解析 song 字段
     */
    public String chatStreamWithToolsAndJsonExtract(List<com.moodtune.dto.LlmMessage> messages,
                                                    SseEmitter emitter,
                                                    List<ToolCallback> toolCallbacks) throws IOException {
        List<Message> springAiMessages = messages.stream()
                .map(this::convertMessage)
                .collect(Collectors.toList());

        StringBuilder rawBuffer = new StringBuilder();

        // 状态机状态
        // 0 = 还未找到 "text":" 开始位置
        // 1 = 正在推送 text 字段内容
        // 2 = text 字段已结束，只缓冲剩余内容
        final int[] state = {0};
        // 用于在状态0时匹配 "text":" 的滑动窗口
        final String TEXT_FIELD_MARKER = "\"text\":\"";
        // 追踪已推送的 text 内容，用于检测结束符
        final StringBuilder textBuffer = new StringBuilder();

        try {
            var client = customChatClientBuilder.build();
            var promptBuilder = client.prompt().messages(springAiMessages);
            if (toolCallbacks != null && !toolCallbacks.isEmpty()) {
                promptBuilder.toolCallbacks(toolCallbacks);
            }

            Flux<String> contentStream = promptBuilder
                    .stream()
                    .content()
                    .timeout(Duration.ofSeconds(60));

            contentStream
                    .doOnNext(chunk -> {
                        rawBuffer.append(chunk);

                        if (state[0] == 0) {
                            // 检查是否已经出现了 "text":" 标记
                            String bufferStr = rawBuffer.toString();
                            int markerIdx = bufferStr.indexOf(TEXT_FIELD_MARKER);
                            if (markerIdx >= 0) {
                                state[0] = 1;
                                // 把标记之后的内容开始推送
                                String afterMarker = bufferStr.substring(markerIdx + TEXT_FIELD_MARKER.length());
                                if (!afterMarker.isEmpty()) {
                                    String toSend = extractTextChunk(afterMarker, textBuffer, state);
                                    if (!toSend.isEmpty()) {
                                        sendChunk(emitter, toSend);
                                    }
                                }
                            }
                        } else if (state[0] == 1) {
                            String toSend = extractTextChunk(chunk, textBuffer, state);
                            if (!toSend.isEmpty()) {
                                sendChunk(emitter, toSend);
                            }
                        }
                    })
                    .doOnError(TimeoutException.class, e -> {
                        log.error("LLM响应超时", e);
                        try {
                            emitter.send(SseEmitter.event().name("error").data("AI响应超时，请稍后重试"));
                            emitter.complete();
                        } catch (IOException ignored) {}
                    })
                    .doOnError(e -> {
                        if (!(e instanceof TimeoutException)) {
                            log.error("LLM调用失败", e);
                            try {
                                emitter.send(SseEmitter.event().name("error").data("AI服务暂时不可用，请稍后重试"));
                                emitter.complete();
                            } catch (IOException ignored) {}
                        }
                    })
                    .blockLast();

            return rawBuffer.toString();
        } catch (Exception e) {
            log.error("Chat stream with JSON extract failed", e);
            throw new IOException("LLM streaming failed", e);
        }
    }

    /**
     * 从 chunk 中提取属于 text 字段的内容，检测到字段结束时切换状态
     *
     * JSON text 字段结束的标志：遇到未转义的 " 字符
     * 例如：...内容结尾","song":...  或  ...内容结尾"}
     */
    private String extractTextChunk(String chunk, StringBuilder textBuffer, int[] state) {
        StringBuilder toSend = new StringBuilder();
        int i = 0;
        while (i < chunk.length()) {
            char c = chunk.charAt(i);

            if (c == '\\' && i + 1 < chunk.length()) {
                // 转义字符：处理 \" \n \t 等
                char next = chunk.charAt(i + 1);
                switch (next) {
                    case '"' -> toSend.append('"');
                    case 'n' -> toSend.append('\n');
                    case 't' -> toSend.append('\t');
                    case '\\' -> toSend.append('\\');
                    default -> toSend.append(next);
                }
                textBuffer.append(toSend.charAt(toSend.length() - 1));
                i += 2;
            } else if (c == '"') {
                // 未转义的引号 = text 字段结束
                state[0] = 2;
                break;
            } else {
                toSend.append(c);
                textBuffer.append(c);
                i++;
            }
        }
        return toSend.toString();
    }

    private void sendChunk(SseEmitter emitter, String content) {
        try {
            emitter.send(SseEmitter.event().name("message").data(content));
        } catch (IOException e) {
            log.error("发送SSE chunk失败", e);
        }
    }
    public String chatWithTools(List<com.moodtune.dto.LlmMessage> messages,
                                List<ToolCallback> toolCallbacks) {
        List<Message> springAiMessages = messages.stream()
                .map(this::convertMessage)
                .collect(Collectors.toList());

        try {
            var client = customChatClientBuilder.build();
            var promptBuilder = client.prompt().messages(springAiMessages);
            if (toolCallbacks != null && !toolCallbacks.isEmpty()) {
                promptBuilder.toolCallbacks(toolCallbacks);
            }
            return promptBuilder.call().content();
        } catch (Exception e) {
            log.error("Chat with tools call failed", e);
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
