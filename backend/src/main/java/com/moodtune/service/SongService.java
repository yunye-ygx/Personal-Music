package com.moodtune.service;

import com.moodtune.dto.SongDTO;
import com.moodtune.entity.Song;
import com.moodtune.mapper.SongMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SongService {

    private final SongMapper songMapper;
    private final MinioService minioService;

    public List<SongDTO> getAllSongs() {
        return songMapper.selectList(null).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<SongDTO> getLikedSongs() {
        return songMapper.findByLikedTrue().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public SongDTO getSongById(Long id) {
        Song song = songMapper.selectById(id);
        if (song == null) {
            throw new RuntimeException("Song not found: " + id);
        }
        return toDTO(song);
    }

    @Transactional
    public SongDTO addSong(SongDTO dto) {
        Song song = Song.builder()
                .title(dto.getTitle())
                .artist(dto.getArtist())
                .genre(dto.getGenre())
                .fileUrl(dto.getFileUrl())
                .liked(dto.getLiked() != null ? dto.getLiked() : false)
                .createdAt(LocalDateTime.now())
                .build();
        songMapper.insert(song);
        return toDTO(song);
    }

    @Transactional
    public SongDTO toggleLiked(Long id) {
        Song song = songMapper.selectById(id);
        if (song == null) {
            throw new RuntimeException("Song not found: " + id);
        }
        song.setLiked(!song.getLiked());
        songMapper.updateById(song);
        return toDTO(song);
    }

    /**
     * 上传歌曲文件并保存到数据库（一步到位）
     */
    @Transactional
    public SongDTO uploadSong(MultipartFile file, String title, String artist, String genre) {
        // 1. 上传文件到MinIO
        String fileUrl = minioService.uploadFile(file, file.getOriginalFilename());

        // 2. 保存到数据库
        Song song = Song.builder()
                .title(title)
                .artist(artist)
                .genre(genre)
                .fileUrl(fileUrl)
                .liked(false)
                .createdAt(LocalDateTime.now())
                .build();
        songMapper.insert(song);

        // 3. 返回DTO
        return toDTO(song);
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
