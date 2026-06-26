package com.moodtune.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wraps a ToolCallback to capture results from tool calls.
 * Used to extract song data from the streaming LLM response's tool calls.
 */
@Slf4j
public class ToolCallCapture implements ToolCallback {

    private final ToolCallback delegate;
    private final List<String> capturedResults = new ArrayList<>();

    public ToolCallCapture(ToolCallback delegate) {
        this.delegate = delegate;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String input) {
        String result = delegate.call(input);
        capturedResults.add(result);
        log.info("ToolCallCapture - captured result ({} chars)", result.length());
        return result;
    }

    @Override
    public String call(String input, ToolContext toolContext) {
        String result = delegate.call(input, toolContext);
        capturedResults.add(result);
        log.info("ToolCallCapture - captured result ({} chars)", result.length());
        return result;
    }

    public List<String> getCapturedResults() {
        return Collections.unmodifiableList(capturedResults);
    }

    public String getLastResult() {
        return capturedResults.isEmpty() ? null : capturedResults.get(capturedResults.size() - 1);
    }
}
