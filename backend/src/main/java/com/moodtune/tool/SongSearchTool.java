package com.moodtune.tool;

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
public class SongSearchTool implements Function<SongSearchRequest, List<Song>> {

    private final SongMapper songMapper;

    @Override
    public List<Song> apply(SongSearchRequest request) {
        try {
            final SongSearchRequest finalRequest = (request == null) ? new SongSearchRequest() : request;

            QueryWrapper<Song> wrapper = new QueryWrapper<>();
            wrapper.eq("liked", true);

            if (finalRequest.getTitle() != null && !finalRequest.getTitle().trim().isEmpty()) {
                wrapper.like("title", finalRequest.getTitle());
            }

            if (finalRequest.getArtist() != null && !finalRequest.getArtist().trim().isEmpty()) {
                wrapper.like("artist", finalRequest.getArtist());
            }

            if (finalRequest.getGenre() != null && !finalRequest.getGenre().trim().isEmpty()) {
                wrapper.eq("genre", finalRequest.getGenre());
            }

            if (finalRequest.getKeyword() != null && !finalRequest.getKeyword().trim().isEmpty()) {
                final String kw = finalRequest.getKeyword();
                wrapper.and(w -> w
                    .like("title", kw)
                    .or().like("artist", kw)
                    .or().like("genre", kw)
                    .or().apply("? = ANY(mood_tags)", kw)
                );
            }

            wrapper.last("LIMIT 20");

            List<Song> results = songMapper.selectList(wrapper);
            log.info("searchLikedSongs - title={}, artist={}, genre={}, keyword={}, found {} songs",
                     finalRequest.getTitle(), finalRequest.getArtist(),
                     finalRequest.getGenre(), finalRequest.getKeyword(), results.size());
            return results;

        } catch (Exception e) {
            log.error("Failed to search liked songs", e);
            return List.of();
        }
    }
}
