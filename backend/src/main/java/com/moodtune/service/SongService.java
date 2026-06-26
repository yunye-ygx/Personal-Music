package com.moodtune.service;

import com.moodtune.dto.SongDTO;
import com.moodtune.entity.Song;
import com.moodtune.mapper.SongMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongService {

    private final SongMapper songMapper;
    private final MinioService minioService;
    private final MusicGenreService musicGenreService;
    private final SongSyncService songSyncService;

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

    public Song getSongEntityById(Long id) {
        return songMapper.selectById(id);
    }

    public Song findByTitleAndArtist(String title, String artist) {
        log.info("Finding song - title: '{}', artist: '{}'", title, artist);
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Song>();
        wrapper.eq("title", title);
        if (artist != null && !artist.isBlank()) {
            wrapper.eq("artist", artist);
        }
        wrapper.last("LIMIT 1");
        Song result = songMapper.selectOne(wrapper);
        log.info("Query result: {}", result != null ? "FOUND (id=" + result.getId() + ")" : "NOT FOUND");
        return result;
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
        songSyncService.syncSong(song);
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
        songSyncService.syncSong(song);
        return toDTO(song);
    }

    /**
     * 上传本地音乐文件（真正的导入）
     */
    @Transactional
    public SongDTO uploadLocalSong(MultipartFile file, String title, String artist, String genre) {
        try {
            // 1. 生成文件名
            String originalFilename = file.getOriginalFilename();
            String fileName = title + "_" + artist + getFileExtension(originalFilename);

            // 2. 上传到MinIO
            String fileUrl = minioService.uploadFile(file, fileName);

            // 【新增】如果 genre 为空或"未知"，使用 AI 识别
            if (genre == null || genre.isEmpty() || "未知".equals(genre)) {
                genre = musicGenreService.predictGenre(title, artist);
            }

            // 3. 保存到数据库
            Song song = Song.builder()
                    .title(title)
                    .artist(artist)
                    .genre(genre)
                    .fileUrl(fileUrl)
                    .liked(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            songMapper.insert(song);
            songSyncService.syncSong(song);

            // 4. 返回DTO
            return toDTO(song);

        } catch (Exception e) {
            throw new RuntimeException("上传本地音乐失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".mp3";  // 默认mp3
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * 上传歌曲文件并保存到数据库（一步到位）
     * @deprecated 使用 uploadLocalSong 替代
     */
    @Deprecated
    @Transactional
    public SongDTO uploadSong(MultipartFile file, String title, String artist, String genre) {
        return uploadLocalSong(file, title, artist, genre);
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
