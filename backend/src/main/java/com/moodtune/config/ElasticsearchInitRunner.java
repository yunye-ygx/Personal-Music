package com.moodtune.config;

import com.moodtune.mapper.SongMapper;
import com.moodtune.service.SongSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchInitRunner implements CommandLineRunner {

    private final SongSyncService songSyncService;
    private final SongMapper songMapper;

    @Override
    public void run(String... args) {
        try {
            songSyncService.syncAll(songMapper.selectList(null));
            log.info("ES 初始化同步完成");
        } catch (Exception e) {
            log.warn("ES 初始化同步失败，搜索功能暂不可用: {}", e.getMessage());
        }
    }
}
