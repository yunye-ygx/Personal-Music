package com.moodtune.service;

import com.moodtune.dto.SongDTO;
import com.moodtune.entity.Song;
import com.moodtune.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SongService {

    private final SongRepository songRepository;

    public List<SongDTO> getAllSongs() {
        return songRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<SongDTO> getLikedSongs() {
        return songRepository.findByLikedTrue().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public SongDTO getSongById(Long id) {
        Song song = songRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Song not found: " + id));
        return toDTO(song);
    }

    @Transactional
    public SongDTO addSong(SongDTO dto) {
        Song song = new Song();
        song.setTitle(dto.getTitle());
        song.setArtist(dto.getArtist());
        song.setGenre(dto.getGenre());
        song.setFileUrl(dto.getFileUrl());
        song.setLiked(dto.getLiked() != null ? dto.getLiked() : false);
        return toDTO(songRepository.save(song));
    }

    @Transactional
    public SongDTO toggleLiked(Long id) {
        Song song = songRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Song not found: " + id));
        song.setLiked(!song.getLiked());
        return toDTO(songRepository.save(song));
    }

    private SongDTO toDTO(Song song) {
        SongDTO dto = new SongDTO();
        dto.setId(song.getId());
        dto.setTitle(song.getTitle());
        dto.setArtist(song.getArtist());
        dto.setGenre(song.getGenre());
        dto.setFileUrl(song.getFileUrl());
        dto.setLiked(song.getLiked());
        return dto;
    }
}
