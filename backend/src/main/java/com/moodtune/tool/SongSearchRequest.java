package com.moodtune.tool;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Search request parameters for liked songs.
 * All fields are optional - can be null or empty.
 */
@Data
@NoArgsConstructor
public class SongSearchRequest {

    /**
     * Song title for exact/fuzzy match. Example: 晴天, 七里香
     */
    private String title;

    /**
     * Artist name for exact/fuzzy match. Example: 周杰伦, 陈奕迅
     */
    private String artist;

    /**
     * Music genre for exact match. Example: 民谣, 摇滚, 流行, 爵士
     */
    private String genre;

    /**
     * Keyword for fuzzy search across title, artist, genre, and mood tags.
     * Example: 安静, 伤感, 治愈, 激昂
     */
    private String keyword;
}
