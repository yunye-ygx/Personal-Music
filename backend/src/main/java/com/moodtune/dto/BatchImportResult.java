package com.moodtune.dto;

import lombok.Data;
import java.util.List;

/**
 * 批量导入结果
 */
@Data
public class BatchImportResult {
    private Integer total;          // 总数
    private Integer successCount;   // 成功数量
    private Integer failCount;      // 失败数量
    private List<FailedSong> failedSongs;  // 失败的歌曲列表

    @Data
    public static class FailedSong {
        private Long songId;
        private String title;
        private String artist;
        private String reason;  // 失败原因

        public FailedSong(Long songId, String title, String artist, String reason) {
            this.songId = songId;
            this.title = title;
            this.artist = artist;
            this.reason = reason;
        }
    }
}
