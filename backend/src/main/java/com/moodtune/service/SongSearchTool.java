package com.moodtune.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.moodtune.entity.Song;
import com.moodtune.mapper.SongMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

/**
 * Song search tool for Spring AI function calling.
 * Searches only liked songs (red heart mode) with optional filters.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SongSearchTool implements Function<SongSearchTool.SearchRequest, List<Song>> {

    private final SongMapper songMapper;

    /**
     * Search liked songs with optional filters.
     * All parameters are optional - if none provided, returns all liked songs.
     *
     * @param request Search parameters
     * @return List of matching songs (max 20)
     */
    @Override
    public List<Song> apply(SearchRequest request) {
        try {
            QueryWrapper<Song> wrapper = new QueryWrapper<>();
            // Strict red heart mode - always filter by liked = true
            wrapper.eq("liked", true);

            // Optional: filter by title (fuzzy match)
            if (request.title != null && !request.title.trim().isEmpty()) {
                wrapper.like("title", request.title);
            }

            // Optional: filter by artist (fuzzy match)
            if (request.artist != null && !request.artist.trim().isEmpty()) {
                wrapper.like("artist", request.artist);
            }

            // Optional: filter by genre (exact match)
            if (request.genre != null && !request.genre.trim().isEmpty()) {
                wrapper.eq("genre", request.genre);
            }

            // Optional: keyword search across all fields including mood_tags
            if (request.keyword != null && !request.keyword.trim().isEmpty()) {
                wrapper.and(w -> w
                    .like("title", request.keyword)
                    .or().like("artist", request.keyword)
                    .or().like("genre", request.keyword)
                    .or().apply("? = ANY(mood_tags)", request.keyword)
                );
            }

            // Limit results to 20 songs
            wrapper.last("LIMIT 20");

            List<Song> results = songMapper.selectList(wrapper);
            log.info("searchLikedSongs - title={}, artist={}, genre={}, keyword={}, found {} songs",
                     request.title, request.artist, request.genre, request.keyword, results.size());
            return results;

        } catch (Exception e) {
            log.error("Failed to search liked songs", e);
            throw new RuntimeException("歌曲查询失败，请稍后重试");
        }
    }

    /**
     * Search request parameters for liked songs.
     * All fields are optional.
     */
    public static class SearchRequest {
        /**
         * Song title for exact/fuzzy match. Example: 晴天, 七里香
         */
        public String title;

        /**
         * Artist name for exact/fuzzy match. Example: 周杰伦, 陈奕迅
         */
        public String artist;

        /**
         * Music genre for exact match. Example: 民谣, 摇滚, 流行, 爵士
         */
        public String genre;

        /**
         * Keyword for fuzzy search across title, artist, genre, and mood tags.
         * Example: 安静, 伤感, 治愈, 激昂
         */
        public String keyword;
    }
}
