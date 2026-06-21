package com.moodtune.controller;

import com.moodtune.service.MusicGenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 测试控制器
 */
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final MusicGenreService musicGenreService;

    /**
     * 测试 AI 识别音乐风格
     * GET /api/test/genre?title=晴天&artist=周杰伦
     */
    @GetMapping("/genre")
    public ResponseEntity<Map<String, String>> testGenrePrediction(
            @RequestParam String title,
            @RequestParam String artist
    ) {
        String genre = musicGenreService.predictGenre(title, artist);

        return ResponseEntity.ok(Map.of(
                "title", title,
                "artist", artist,
                "genre", genre
        ));
    }
}
