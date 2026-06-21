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
@TableName("songs")
public class Song {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String artist;

    private String genre;

    @TableField("file_url")
    private String fileUrl;

    @Builder.Default
    private Boolean liked = false;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
