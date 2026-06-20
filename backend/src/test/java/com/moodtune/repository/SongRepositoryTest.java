package com.moodtune.repository;

import com.moodtune.entity.Song;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class SongRepositoryTest {

    @Autowired
    private SongRepository songRepository;

    @Test
    void saveAndFindSong() {
        Song song = new Song();
        song.setTitle("爱错");
        song.setArtist("王力宏");
        song.setGenre("流行");
        song.setFileUrl("http://minio/test.mp3");
        song.setLiked(true);

        Song saved = songRepository.save(song);
        assertNotNull(saved.getId());
        assertEquals("爱错", saved.getTitle());
    }

    @Test
    void findLikedSongs() {
        Song s1 = new Song();
        s1.setTitle("Song A");
        s1.setArtist("Artist A");
        s1.setLiked(true);
        s1.setFileUrl("http://minio/a.mp3");
        songRepository.save(s1);

        Song s2 = new Song();
        s2.setTitle("Song B");
        s2.setArtist("Artist B");
        s2.setLiked(false);
        s2.setFileUrl("http://minio/b.mp3");
        songRepository.save(s2);

        List<Song> liked = songRepository.findByLikedTrue();
        assertEquals(1, liked.size());
        assertEquals("Song A", liked.get(0).getTitle());
    }
}
