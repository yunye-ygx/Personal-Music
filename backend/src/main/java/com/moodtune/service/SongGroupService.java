package com.moodtune.service;

import com.moodtune.dto.SongDTO;
import com.moodtune.dto.SongGroupDTO;
import com.moodtune.entity.SongGroup;
import com.moodtune.entity.SongGroupRelation;
import com.moodtune.repository.SongGroupRelationRepository;
import com.moodtune.repository.SongGroupRepository;
import com.moodtune.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SongGroupService {

    private final SongGroupRepository groupRepository;
    private final SongGroupRelationRepository relationRepository;
    private final SongRepository songRepository;
    private final SongService songService;

    public List<SongGroupDTO> getAllGroups() {
        return groupRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public SongGroupDTO createGroup(String name, String source) {
        SongGroup group = new SongGroup();
        group.setName(name);
        group.setSource(source);
        return toDTO(groupRepository.save(group));
    }

    @Transactional
    public void addSongToGroup(Long songId, Long groupId) {
        SongGroupRelation rel = new SongGroupRelation();
        rel.setSongId(songId);
        rel.setGroupId(groupId);
        relationRepository.save(rel);
    }

    @Transactional
    public void removeSongFromGroup(Long songId, Long groupId) {
        relationRepository.deleteBySongIdAndGroupId(songId, groupId);
    }

    private SongGroupDTO toDTO(SongGroup group) {
        SongGroupDTO dto = new SongGroupDTO();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setSource(group.getSource());

        List<SongGroupRelation> relations = relationRepository.findByGroupId(group.getId());
        List<SongDTO> songs = relations.stream()
                .map(rel -> songService.getSongById(rel.getSongId()))
                .collect(Collectors.toList());
        dto.setSongs(songs);

        return dto;
    }
}
