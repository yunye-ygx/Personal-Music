package com.moodtune.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

/**
 * 网易云音乐搜索结果
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeteaseSearchResult {

    private Integer code;
    private Result result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private List<Song> songs;
        private Integer songCount;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Song {
        private Long id;
        private String name;
        private List<Artist> artists;
        private Album album;
        private Integer duration; // 毫秒

        public String getArtistNames() {
            if (artists == null || artists.isEmpty()) {
                return "未知艺术家";
            }
            return String.join(", ", artists.stream()
                    .map(Artist::getName)
                    .toList());
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Artist {
        private Long id;
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Album {
        private Long id;
        private String name;
    }
}
