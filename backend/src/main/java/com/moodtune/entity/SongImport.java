package com.moodtune.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "song_imports")
public class SongImport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_link", nullable = false)
    private String sourceLink;

    @Column(nullable = false)
    private String platform; // "qq_music", "netease", etc.

    @Column(nullable = false)
    private String status; // "pending", "success", "failed"

    @Column(name = "imported_count")
    private Integer importedCount = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
