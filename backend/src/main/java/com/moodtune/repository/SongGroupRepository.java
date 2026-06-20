package com.moodtune.repository;

import com.moodtune.entity.SongGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SongGroupRepository extends JpaRepository<SongGroup, Long> {
    List<SongGroup> findBySource(String source);
}
