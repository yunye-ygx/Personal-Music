package com.moodtune.repository;

import com.moodtune.entity.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SongRepository extends JpaRepository<Song, Long> {
    List<Song> findByLikedTrue();
    List<Song> findByGenre(String genre);
    List<Song> findByTitleContainingOrArtistContaining(String title, String artist);
}
