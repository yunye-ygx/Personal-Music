package com.moodtune.controller;

import com.moodtune.dto.SongDTO;
import com.moodtune.service.SongSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SongSearchService songSearchService;

    @GetMapping
    public ResponseEntity<List<SongDTO>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(songSearchService.search(keyword));
    }
}
