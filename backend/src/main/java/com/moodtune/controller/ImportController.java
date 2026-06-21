package com.moodtune.controller;

import com.moodtune.dto.NeteaseSearchResult;
import com.moodtune.dto.SongDTO;
import com.moodtune.service.NeteaseCrawlerService;
import com.moodtune.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 导入控制器 - 支持两种导入方式：
 * 1. 上传本地音乐文件
 * 2. 从网易云音乐平台下载
 */
@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final NeteaseCrawlerService neteaseCrawlerService;
    private final SongService songService;

    /**
     * 上传本地音乐文件（真正的导入）
     *
     * @param file   音乐文件
     * @param title  歌曲名称
     * @param artist 艺术家
     * @return 导入的歌曲信息
     */
    @PostMapping("/upload")
    public ResponseEntity<SongDTO> uploadLocalFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("artist") String artist,
            @RequestParam(value = "genre", required = false, defaultValue = "未知") String genre
    ) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("歌曲名称不能为空");
        }

        if (artist == null || artist.trim().isEmpty()) {
            throw new IllegalArgumentException("艺术家名称不能为空");
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("audio/")) {
            throw new IllegalArgumentException("只支持音频文件");
        }

        SongDTO song = songService.uploadLocalSong(file, title, artist, genre);
        return ResponseEntity.ok(song);
    }

    /**
     * 搜索网易云音乐（预览）
     *
     * @param keyword 搜索关键词
     * @return 搜索结果列表
     */
    @GetMapping("/netease/search")
    public ResponseEntity<List<NeteaseSearchResult.Song>> searchNetease(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("搜索关键词不能为空");
        }

        List<NeteaseSearchResult.Song> results = neteaseCrawlerService.searchSongs(keyword);
        return ResponseEntity.ok(results);
    }

    /**
     * 从网易云下载音乐（通过分享链接）
     * 支持单曲和歌单链接
     *
     * @param body {url: "https://music.163.com/#/song?id=123456" 或 "https://music.163.com/m/playlist?id=123456"}
     * @return 单曲返回 SongDTO，歌单返回 BatchImportResult
     */
    @PostMapping("/netease/download")
    public ResponseEntity<?> downloadFromNetease(@RequestBody Map<String, String> body) {
        String url = body.get("url");

        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("分享链接不能为空");
        }

        Object result = neteaseCrawlerService.importByUrl(url);
        return ResponseEntity.ok(result);
    }
}

