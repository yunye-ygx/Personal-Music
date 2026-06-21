package com.moodtune.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("song_imports")
public class SongImport {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("source_link")
    private String sourceLink;

    private String platform;

    private String status;

    @TableField("imported_count")
    @Builder.Default
    private Integer importedCount = 0;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
