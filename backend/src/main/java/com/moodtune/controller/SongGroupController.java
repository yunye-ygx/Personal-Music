package com.moodtune.controller;

import com.moodtune.dto.SongGroupDTO;
import com.moodtune.service.SongGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/song-groups")
@RequiredArgsConstructor
public class SongGroupController {

    private final SongGroupService groupService;

    @GetMapping
    public ResponseEntity<List<SongGroupDTO>> getAllGroups() {
        return ResponseEntity.ok(groupService.getAllGroups());
    }

    @PostMapping
    public ResponseEntity<SongGroupDTO> createGroup(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String source = body.getOrDefault("source", "manual");
        return ResponseEntity.ok(groupService.createGroup(name, source));
    }

    @PostMapping("/{groupId}/songs/{songId}")
    public ResponseEntity<Void> addSongToGroup(@PathVariable Long groupId, @PathVariable Long songId) {
        groupService.addSongToGroup(songId, groupId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{groupId}/songs/{songId}")
    public ResponseEntity<Void> removeSongFromGroup(@PathVariable Long groupId, @PathVariable Long songId) {
        groupService.removeSongFromGroup(songId, groupId);
        return ResponseEntity.ok().build();
    }
}
