package com.moodtune.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "song_group_relations")
public class SongGroupRelation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "song_id", nullable = false)
    private Long songId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;
}
