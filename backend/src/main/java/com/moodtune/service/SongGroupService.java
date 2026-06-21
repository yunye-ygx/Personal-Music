package com.moodtune.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moodtune.dto.SongDTO;
import com.moodtune.dto.SongGroupDTO;
import com.moodtune.entity.SongGroup;
import com.moodtune.entity.SongGroupRelation;
import com.moodtune.mapper.SongGroupMapper;
import com.moodtune.mapper.SongGroupRelationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SongGroupService {

    private final SongGroupMapper groupMapper;
    private final SongGroupRelationMapper relationMapper;
    private final SongService songService;

    public List<SongGroupDTO> getAllGroups() {
        return groupMapper.selectList(null).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public SongGroupDTO createGroup(String name, String source) {
        SongGroup group = SongGroup.builder()
                .name(name)
                .source(source)
                .createdAt(LocalDateTime.now())
                .build();
        groupMapper.insert(group);
        return toDTO(group);
    }

    @Transactional
    public void addSongToGroup(Long songId, Long groupId) {
        SongGroupRelation rel = SongGroupRelation.builder()
                .songId(songId)
                .groupId(groupId)
                .build();
        relationMapper.insert(rel);
    }

    @Transactional
    public void removeSongFromGroup(Long songId, Long groupId) {
        LambdaQueryWrapper<SongGroupRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SongGroupRelation::getSongId, songId)
               .eq(SongGroupRelation::getGroupId, groupId);
        relationMapper.delete(wrapper);
    }

    private SongGroupDTO toDTO(SongGroup group) {
        SongGroupDTO dto = new SongGroupDTO();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setSource(group.getSource());

        List<SongGroupRelation> relations = relationMapper.findByGroupId(group.getId());
        List<SongDTO> songs = relations.stream()
                .map(rel -> songService.getSongById(rel.getSongId()))
                .collect(Collectors.toList());
        dto.setSongs(songs);

        return dto;
    }
}
