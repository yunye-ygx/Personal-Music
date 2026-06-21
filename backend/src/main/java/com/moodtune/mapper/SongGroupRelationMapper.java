package com.moodtune.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moodtune.entity.SongGroupRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface SongGroupRelationMapper extends BaseMapper<SongGroupRelation> {

    @Select("SELECT * FROM song_group_relations WHERE group_id = #{groupId}")
    List<SongGroupRelation> findByGroupId(Long groupId);

    @Select("SELECT * FROM song_group_relations WHERE song_id = #{songId}")
    List<SongGroupRelation> findBySongId(Long songId);
}
