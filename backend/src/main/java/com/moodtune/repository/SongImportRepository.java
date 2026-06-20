package com.moodtune.repository;

import com.moodtune.entity.SongImport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SongImportRepository extends JpaRepository<SongImport, Long> {
    List<SongImport> findByStatus(String status);
}
