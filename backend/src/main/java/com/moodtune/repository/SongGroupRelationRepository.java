package com.moodtune.repository;

import com.moodtune.entity.SongGroupRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface SongGroupRelationRepository extends JpaRepository<SongGroupRelation, Long> {
    List<SongGroupRelation> findByGroupId(Long groupId);
    List<SongGroupRelation> findBySongId(Long songId);
    @Transactional
    void deleteBySongIdAndGroupId(Long songId, Long groupId);
}
