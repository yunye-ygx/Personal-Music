package com.moodtune.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("song_group_relations")
public class SongGroupRelation {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("song_id")
    private Long songId;

    @TableField("group_id")
    private Long groupId;
}
