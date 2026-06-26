package com.moodtune.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.moodtune.document.SongDocument;
import com.moodtune.entity.Song;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongSyncService {

    private final ElasticsearchClient esClient;
    private final ElasticsearchOperations esOperations;

    public void initIndex() {
        IndexOperations indexOps = esOperations.indexOps(IndexCoordinates.of("songs"));
        if (!indexOps.exists()) {
            indexOps.create();
            indexOps.putMapping(indexOps.createMapping());
            log.info("ES 索引 [songs] 创建成功");
        }
    }

    public void syncSong(Song song) {
        try {
            esClient.index(i -> i
                    .index("songs")
                    .id(song.getId().toString())
                    .document(toDocument(song))
            );
            log.info("歌曲同步到 ES: id={}, title={}", song.getId(), song.getTitle());
        } catch (IOException e) {
            log.error("同步歌曲到 ES 失败: id={}", song.getId(), e);
        }
    }

    public void deleteSong(Long id) {
        try {
            esClient.delete(d -> d.index("songs").id(id.toString()));
            log.info("从 ES 删除歌曲: id={}", id);
        } catch (IOException e) {
            log.error("从 ES 删除歌曲失败: id={}", id, e);
        }
    }

    public void syncAll(List<Song> songs) {
        initIndex();
        try {
            BulkRequest.Builder br = new BulkRequest.Builder();
            for (Song song : songs) {
                br.operations(op -> op
                        .index(idx -> idx
                                .index("songs")
                                .id(song.getId().toString())
                                .document(toDocument(song))
                        )
                );
            }
            co.elastic.clients.elasticsearch.core.BulkResponse result = esClient.bulk(br.build());
            if (result.errors()) {
                for (BulkResponseItem item : result.items()) {
                    if (item.error() != null) {
                        log.error("批量同步错误: {}", item.error().reason());
                    }
                }
            }
            log.info("批量同步 {} 首歌曲到 ES", songs.size());
        } catch (IOException e) {
            log.error("批量同步到 ES 失败", e);
        }
    }

    private SongDocument toDocument(Song song) {
        return SongDocument.builder()
                .id(song.getId())
                .title(song.getTitle())
                .artist(song.getArtist())
                .genre(song.getGenre())
                .fileUrl(song.getFileUrl())
                .liked(song.getLiked())
                .moodTags(song.getMoodTags())
                .build();
    }
}
