package com.moodtune.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

/**
 * 网易云音乐歌曲详情（含播放URL）
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeteaseSongDetail {

    private Integer code;
    private List<SongData> data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SongData {
        private Long id;
        private String url;  // 播放地址
        private Integer br;  // 比特率
        private String type; // 格式 (mp3, flac等)
        private Long size;   // 文件大小
    }
}
