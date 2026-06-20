package com.moodtune.controller;

import com.moodtune.dto.SongDTO;
import com.moodtune.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/songs")
@RequiredArgsConstructor
public class SongController {

    private final SongService songService;

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
}
