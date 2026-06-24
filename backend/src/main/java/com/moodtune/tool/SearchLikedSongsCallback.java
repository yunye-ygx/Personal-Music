package com.moodtune.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moodtune.entity.Song;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;

/**
 * Custom FunctionCallback for searchLikedSongs that handles empty/null arguments.
 *
 * Spring AI 1.0.0-M5's {@code AbstractFunctionCallback.fromJson()} throws
 * {@code MismatchedInputException} when the LLM sends empty arguments,
 * because {@code ObjectMapper.readValue("", Class)} has no content to parse.
 * This callback wraps the actual tool with defensive deserialization.
 */
@Slf4j
public class SearchLikedSongsCallback implements ToolCallback {

    private static final String NAME = "searchLikedSongs";

    private static final String DESCRIPTION =
            "Search the user's liked songs (red heart playlist) with optional filters "
            + "for title, artist, genre, or mood keywords. Only call this when the user "
            + "explicitly asks for song recommendations.";

    private static final String INPUT_TYPE_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "title": {
                  "type": "string",
                  "description": "Song title for exact/fuzzy match. Example: 晴天, 七里香"
                },
                "artist": {
                  "type": "string",
                  "description": "Artist name for exact/fuzzy match. Example: 周杰伦, 陈奕迅"
                },
                "genre": {
                  "type": "string",
                  "description": "Music genre for exact match. Example: 民谣, 摇滚, 流行, 爵士"
                },
                "keyword": {
                  "type": "string",
                  "description": "Keyword for fuzzy search across title, artist, genre, and mood tags. Example: 安静, 伤感, 治愈, 激昂"
                }
              },
              "additionalProperties": false
            }
            """;

    private final SongSearchTool songSearchTool;
    private final ObjectMapper objectMapper;
    private final ToolDefinition toolDefinition;

    public SearchLikedSongsCallback(SongSearchTool songSearchTool, ObjectMapper objectMapper) {
        this.songSearchTool = songSearchTool;
        this.objectMapper = objectMapper;
        this.toolDefinition = DefaultToolDefinition.builder()
                .name(NAME)
                .description(DESCRIPTION)
                .inputSchema(INPUT_TYPE_SCHEMA)
                .build();
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return toolDefinition;
    }

    @Override
    public String call(String input) {
        return call(input, null);
    }

    @Override
    public String call(String input, ToolContext toolContext) {
        SongSearchRequest request = deserializeRequest(input);
        List<Song> results = songSearchTool.apply(request);
        try {
            return objectMapper.writeValueAsString(results);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize searchLikedSongs results", e);
            return "[]";
        }
    }

    private SongSearchRequest deserializeRequest(String input) {
        if (input == null || input.isBlank()) {
            return new SongSearchRequest();
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty() || "{}".equals(trimmed) || "\"\"".equals(trimmed)) {
            return new SongSearchRequest();
        }
        try {
            return objectMapper.readValue(trimmed, SongSearchRequest.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize searchLikedSongs arguments '{}', using defaults", trimmed, e);
            return new SongSearchRequest();
        }
    }
}
