package com.moodtune.service;

import com.moodtune.dto.SongDTO;
import com.moodtune.entity.Song;
import com.moodtune.repository.SongRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SongServiceTest {

    @Autowired
    private SongService songService;

    @Autowired
    private SongRepository songRepository;

    @Test
    void addAndGetSong() {
        SongDTO dto = new SongDTO();
        dto.setTitle("测试歌曲");
        dto.setArtist("测试歌手");
        dto.setGenre("流行");
        dto.setFileUrl("http://minio/test.mp3");

        SongDTO saved = songService.addSong(dto);
        assertNotNull(saved.getId());
        assertEquals("测试歌曲", saved.getTitle());
        assertFalse(saved.getLiked());
    }

    @Test
    void toggleLiked() {
        Song song = new Song();
        song.setTitle("Toggle Test");
        song.setArtist("Artist");
        song.setFileUrl("http://minio/toggle.mp3");
        song.setLiked(false);
        Song saved = songRepository.save(song);

        SongDTO toggled = songService.toggleLiked(saved.getId());
        assertTrue(toggled.getLiked());

        SongDTO toggledAgain = songService.toggleLiked(saved.getId());
        assertFalse(toggledAgain.getLiked());
    }
}
