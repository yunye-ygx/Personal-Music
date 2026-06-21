package com.moodtune.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moodtune.dto.BatchImportResult;
import com.moodtune.dto.NeteaseSongDetail;
import com.moodtune.dto.NeteaseSearchResult;
import com.moodtune.dto.SongDTO;
import com.moodtune.entity.Song;
import com.moodtune.mapper.SongMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 网易云音乐爬取服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NeteaseCrawlerService {

    private final WebClient.Builder webClientBuilder;
    private final MinioService minioService;
    private final SongMapper songMapper;
    private final ObjectMapper objectMapper;
    private final MusicGenreService musicGenreService;

    private static final String SEARCH_API = "https://music.163.com/api/search/get/web";
    private static final String SONG_URL_API = "https://music.163.com/api/song/enhance/player/url";

    /**
     * 创建WebClient
     */
    private WebClient createWebClient() {
        return webClientBuilder
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .defaultHeader("Referer", "https://music.163.com/")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(50 * 1024 * 1024)) // 50MB
                .build();
    }

    /**
     * 搜索歌曲
     */
    public List<NeteaseSearchResult.Song> searchSongs(String keyword) {
        try {
            WebClient client = createWebClient();

            String response = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("music.163.com")
                            .path("/api/search/get/web")
                            .queryParam("s", keyword)
                            .queryParam("type", 1)  // 1 = 单曲
                            .queryParam("limit", 20)
                            .queryParam("offset", 0)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("网易云搜索响应: {}", response);

            NeteaseSearchResult result = objectMapper.readValue(response, NeteaseSearchResult.class);

            if (result.getCode() == 200 && result.getResult() != null) {
                return result.getResult().getSongs();
            } else {
                log.error("网易云搜索失败，响应码: {}", result.getCode());
                return List.of();
            }
        } catch (Exception e) {
            log.error("搜索网易云音乐失败", e);
            return List.of();
        }
    }

    /**
     * 获取歌曲播放URL
     */
    public String getSongUrl(Long songId) {
        try {
            WebClient client = createWebClient();

            String response = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("music.163.com")
                            .path("/api/song/enhance/player/url")
                            .queryParam("ids", "[" + songId + "]")
                            .queryParam("br", 320000)  // 320kbps
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("网易云播放地址响应: {}", response);

            NeteaseSongDetail result = objectMapper.readValue(response, NeteaseSongDetail.class);

            if (result.getCode() == 200 && result.getData() != null && !result.getData().isEmpty()) {
                String url = result.getData().get(0).getUrl();
                if (url != null && !url.isEmpty()) {
                    return url;
                }
            }

            log.error("获取播放地址失败，可能是VIP歌曲或版权受限");
            return null;

        } catch (Exception e) {
            log.error("获取网易云音频URL失败", e);
            return null;
        }
    }

    /**
     * 下载音频并上传到MinIO
     */
    public String downloadAndUpload(String audioUrl, String fileName) {
        try {
            WebClient client = createWebClient();

            log.info("开始下载音频: {}", audioUrl);

            // 下载音频为字节数组
            byte[] audioBytes = client.get()
                    .uri(audioUrl)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();

            if (audioBytes == null || audioBytes.length == 0) {
                throw new RuntimeException("下载音频流失败");
            }

            log.info("音频下载完成，大小: {} bytes", audioBytes.length);

            // 转换为InputStream并上传到MinIO
            InputStream audioStream = new java.io.ByteArrayInputStream(audioBytes);
            String minioUrl = minioService.uploadStream(audioStream, fileName);

            log.info("音频上传成功: {}", minioUrl);
            return minioUrl;

        } catch (Exception e) {
            log.error("下载并上传音频失败", e);
            throw new RuntimeException("下载音频失败: " + e.getMessage());
        }
    }

    /**
     * 解析网易云分享链接，提取歌曲ID
     */
    public Long parseSongIdFromUrl(String url) {
        try {
            // 支持格式：
            // https://music.163.com/#/song?id=123456
            // https://music.163.com/song?id=123456
            // https://y.music.163.com/m/song?id=123456

            if (url.contains("id=")) {
                String idPart = url.substring(url.indexOf("id=") + 3);

                // 去掉后面可能的参数
                if (idPart.contains("&")) {
                    idPart = idPart.substring(0, idPart.indexOf("&"));
                }
                if (idPart.contains("#")) {
                    idPart = idPart.substring(0, idPart.indexOf("#"));
                }

                return Long.parseLong(idPart.trim());
            }

            throw new IllegalArgumentException("无法从链接中解析出歌曲ID");

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("链接中的ID格式不正确");
        }
    }

    /**
     * 通过链接导入歌曲（支持单曲和歌单）
     * 注意：歌单导入返回的 SongDTO 包含批量导入统计信息
     */
    @Transactional
    public Object importByUrl(String url) {
        try {
            // 判断链接类型
            if (isPlaylistUrl(url)) {
                // 歌单链接 - 批量导入，返回 BatchImportResult
                return importPlaylistByUrl(url);
            } else {
                // 单曲链接 - 导入单曲，返回 SongDTO
                return importSingleSongByUrl(url);
            }
        } catch (Exception e) {
            log.error("通过链接导入失败: {}", url, e);
            throw new RuntimeException("导入失败: " + e.getMessage());
        }
    }

    /**
     * 判断是否为歌单链接
     */
    private boolean isPlaylistUrl(String url) {
        return url.contains("/playlist?") || url.contains("/playlist/");
    }

    /**
     * 导入单曲
     */
    private SongDTO importSingleSongByUrl(String url) {
        try {
            // 1. 解析链接获取ID
            Long songId = parseSongIdFromUrl(url);
            log.info("从链接解析出歌曲ID: {}", songId);

            // 2. 获取播放URL
            String audioUrl = getSongUrl(songId);
            if (audioUrl == null || audioUrl.isEmpty()) {
                throw new RuntimeException("无法获取音频下载地址，可能是VIP歌曲或版权受限");
            }

            // 3. 获取歌曲详细信息（使用歌曲详情 API）
            NeteaseSearchResult.Song song = getSongDetail(songId);

            String title;
            String artist;
            String genre;

            if (song != null) {
                title = song.getName();
                artist = song.getArtistNames();
                // 使用 AI 识别风格
                genre = musicGenreService.predictGenre(title, artist);
                log.info("AI 识别风格: {} - {} → {}", title, artist, genre);
            } else {
                // 如果获取详情失败，使用默认值
                title = "未知歌曲_" + songId;
                artist = "未知艺术家";
                genre = "未知";
                log.warn("无法获取歌曲详情，ID: {}", songId);
            }

            // 4. 下载并上传到MinIO
            String fileName = title + "_" + artist + ".mp3";
            String minioUrl = downloadAndUpload(audioUrl, fileName);

            // 5. 保存到数据库
            Song dbSong = Song.builder()
                    .title(title)
                    .artist(artist)
                    .genre(genre)
                    .fileUrl(minioUrl)
                    .liked(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            songMapper.insert(dbSong);

            // 6. 返回DTO
            return toDTO(dbSong);

        } catch (Exception e) {
            log.error("导入单曲失败", e);
            throw new RuntimeException("导入失败: " + e.getMessage());
        }
    }

    /**
     * 导入歌单（批量导入）
     */
    private BatchImportResult importPlaylistByUrl(String url) {
        try {
            // 1. 解析歌单ID
            Long playlistId = parsePlaylistIdFromUrl(url);
            log.info("从链接解析出歌单ID: {}", playlistId);

            // 2. 获取歌单详情
            List<Long> songIds = getPlaylistSongIds(playlistId);
            log.info("歌单包含 {} 首歌曲", songIds.size());

            if (songIds.isEmpty()) {
                throw new RuntimeException("歌单为空或无法获取");
            }

            // 3. 批量导入每首歌
            int successCount = 0;
            int failCount = 0;
            List<BatchImportResult.FailedSong> failedSongs = new ArrayList<>();

            for (Long songId : songIds) {
                try {
                    log.info("正在导入第 {}/{} 首歌曲，ID: {}", successCount + failCount + 1, songIds.size(), songId);

                    // 构造单曲链接并导入
                    String songUrl = "https://music.163.com/#/song?id=" + songId;
                    importSingleSongByUrl(songUrl);
                    successCount++;

                } catch (Exception e) {
                    log.error("导入歌曲失败，ID: {}, 错误: {}", songId, e.getMessage());
                    failCount++;

                    // 获取歌曲基本信息用于显示
                    try {
                        NeteaseSearchResult.Song song = getSongDetail(songId);
                        String title = song != null ? song.getName() : "未知歌曲";
                        String artist = song != null ? song.getArtistNames() : "未知艺术家";
                        String reason = simplifyErrorMessage(e.getMessage());

                        failedSongs.add(new BatchImportResult.FailedSong(songId, title, artist, reason));
                    } catch (Exception ex) {
                        // 如果连歌曲信息都获取不到，使用ID
                        failedSongs.add(new BatchImportResult.FailedSong(
                            songId,
                            "歌曲ID: " + songId,
                            "",
                            simplifyErrorMessage(e.getMessage())
                        ));
                    }
                }
            }

            log.info("歌单导入完成：成功 {} 首，失败 {} 首", successCount, failCount);

            // 返回批量导入结果
            BatchImportResult result = new BatchImportResult();
            result.setTotal(songIds.size());
            result.setSuccessCount(successCount);
            result.setFailCount(failCount);
            result.setFailedSongs(failedSongs);

            return result;

        } catch (Exception e) {
            log.error("导入歌单失败", e);
            throw new RuntimeException("导入歌单失败: " + e.getMessage());
        }
    }

    /**
     * 简化错误信息，让用户更容易理解
     */
    private String simplifyErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            return "未知错误";
        }

        if (errorMessage.contains("timed out") || errorMessage.contains("Connection refused")) {
            return "网络超时，请稍后重试";
        } else if (errorMessage.contains("VIP") || errorMessage.contains("vip")) {
            return "VIP歌曲，无法下载";
        } else if (errorMessage.contains("版权") || errorMessage.contains("copyright")) {
            return "版权受限";
        } else if (errorMessage.contains("无法获取音频")) {
            return "无法获取播放地址";
        } else {
            // 只返回前50个字符，避免太长
            return errorMessage.length() > 50 ? errorMessage.substring(0, 50) + "..." : errorMessage;
        }
    }

    /**
     * 解析歌单ID
     */
    private Long parsePlaylistIdFromUrl(String url) {
        try {
            if (url.contains("id=")) {
                String idPart = url.substring(url.indexOf("id=") + 3);

                // 去掉后面可能的参数
                if (idPart.contains("&")) {
                    idPart = idPart.substring(0, idPart.indexOf("&"));
                }
                if (idPart.contains("#")) {
                    idPart = idPart.substring(0, idPart.indexOf("#"));
                }

                return Long.parseLong(idPart.trim());
            }

            throw new IllegalArgumentException("无法从链接中解析出歌单ID");

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("链接中的ID格式不正确");
        }
    }

    /**
     * 获取歌单中所有歌曲的ID列表
     */
    private List<Long> getPlaylistSongIds(Long playlistId) {
        try {
            WebClient client = createWebClient();

            String response = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("music.163.com")
                            .path("/api/playlist/detail")
                            .queryParam("id", playlistId)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("网易云歌单详情响应: {}", response.substring(0, Math.min(500, response.length())));

            // 解析响应
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response);
            List<Long> songIds = new java.util.ArrayList<>();

            if (root.has("result") && root.get("result").has("tracks")) {
                for (com.fasterxml.jackson.databind.JsonNode track : root.get("result").get("tracks")) {
                    Long songId = track.get("id").asLong();
                    songIds.add(songId);
                }
            }

            return songIds;

        } catch (Exception e) {
            log.error("获取歌单详情失败: {}", playlistId, e);
            throw new RuntimeException("获取歌单失败: " + e.getMessage());
        }
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

    /**
     * 导入指定歌曲（推荐）
     */
    @Transactional
    public SongDTO importBySong(NeteaseSearchResult.Song song) {
        try {
            // 1. 获取播放URL
            String audioUrl = getSongUrl(song.getId());
            if (audioUrl == null || audioUrl.isEmpty()) {
                throw new RuntimeException("无法获取音频下载地址，可能是VIP歌曲或版权受限");
            }

            // 2. 下载并上传到MinIO
            String fileName = song.getName() + "_" + song.getArtistNames() + ".mp3";
            String minioUrl = downloadAndUpload(audioUrl, fileName);

            // 3. 保存到数据库
            Song dbSong = Song.builder()
                    .title(song.getName())
                    .artist(song.getArtistNames())
                    .genre("未知")
                    .fileUrl(minioUrl)
                    .liked(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            songMapper.insert(dbSong);

            // 4. 返回DTO
            SongDTO dto = new SongDTO();
            dto.setId(dbSong.getId());
            dto.setTitle(dbSong.getTitle());
            dto.setArtist(dbSong.getArtist());
            dto.setGenre(dbSong.getGenre());
            dto.setFileUrl(dbSong.getFileUrl());
            dto.setLiked(dbSong.getLiked());

            return dto;

        } catch (Exception e) {
            log.error("导入歌曲失败: {}", song.getName(), e);
            throw new RuntimeException("导入失败: " + e.getMessage());
        }
    }

    /**
     * 通过关键词搜索并导入第一个结果
     */
    @Transactional
    public SongDTO importByKeyword(String keyword) {
        try {
            // 1. 搜索歌曲
            List<NeteaseSearchResult.Song> songs = searchSongs(keyword);
            if (songs.isEmpty()) {
                throw new RuntimeException("未找到歌曲: " + keyword);
            }

            // 2. 导入第一个结果
            NeteaseSearchResult.Song song = songs.get(0);
            log.info("找到歌曲: {} - {}", song.getName(), song.getArtistNames());

            return importBySong(song);

        } catch (Exception e) {
            log.error("导入失败: {}", keyword, e);
            throw new RuntimeException("导入失败: " + e.getMessage());
        }
    }

    /**
     * 获取歌曲详情（通过歌曲详情 API）
     */
    private NeteaseSearchResult.Song getSongDetail(Long songId) {
        try {
            WebClient client = createWebClient();

            String response = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("music.163.com")
                            .path("/api/song/detail")
                            .queryParam("ids", "[" + songId + "]")
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("网易云歌曲详情响应: {}", response);

            // 解析响应
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response);
            if (root.has("songs") && root.get("songs").isArray() && root.get("songs").size() > 0) {
                com.fasterxml.jackson.databind.JsonNode songNode = root.get("songs").get(0);

                NeteaseSearchResult.Song song = new NeteaseSearchResult.Song();
                song.setId(songNode.get("id").asLong());
                song.setName(songNode.get("name").asText());

                // 解析艺术家
                List<NeteaseSearchResult.Artist> artists = new java.util.ArrayList<>();
                if (songNode.has("artists")) {
                    for (com.fasterxml.jackson.databind.JsonNode artistNode : songNode.get("artists")) {
                        NeteaseSearchResult.Artist artist = new NeteaseSearchResult.Artist();
                        artist.setId(artistNode.get("id").asLong());
                        artist.setName(artistNode.get("name").asText());
                        artists.add(artist);
                    }
                }
                song.setArtists(artists);

                return song;
            }

            return null;

        } catch (Exception e) {
            log.error("获取歌曲详情失败: {}", songId, e);
            return null;
        }
    }
}
