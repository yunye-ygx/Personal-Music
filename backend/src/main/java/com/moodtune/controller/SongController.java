package com.moodtune.controller;

import com.moodtune.dto.SongDTO;
import com.moodtune.service.MinioService;
import com.moodtune.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/songs")
@RequiredArgsConstructor
public class SongController {

    private final SongService songService;
    private final MinioService minioService;

    @GetMapping
    public ResponseEntity<List<SongDTO>> getAllSongs() {
        return ResponseEntity.ok(songService.getAllSongs());
    }

    @GetMapping("/liked")
    public ResponseEntity<List<SongDTO>> getLikedSongs() {
        return ResponseEntity.ok(songService.getLikedSongs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SongDTO> getSong(@PathVariable Long id) {
        return ResponseEntity.ok(songService.getSongById(id));
    }

    @PostMapping
    public ResponseEntity<SongDTO> addSong(@RequestBody SongDTO dto) {
        return ResponseEntity.ok(songService.addSong(dto));
    }

    @PatchMapping("/{id}/like")
    public ResponseEntity<SongDTO> toggleLike(@PathVariable Long id) {
        return ResponseEntity.ok(songService.toggleLiked(id));
    }

    @PostMapping("/upload")
    public ResponseEntity<SongDTO> uploadSong(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("artist") String artist,
            @RequestParam(value = "genre", required = false) String genre) {
        String fileUrl = minioService.uploadFile(file, file.getOriginalFilename());
        SongDTO dto = new SongDTO();
        dto.setTitle(title);
        dto.setArtist(artist);
        dto.setGenre(genre);
        dto.setFileUrl(fileUrl);
        return ResponseEntity.ok(songService.addSong(dto));
    }
}
