package com.moodtune.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.moodtune.document.SongDocument;
import com.moodtune.dto.SongDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongSearchService {

    private final ElasticsearchClient esClient;

    public List<SongDTO> search(String keyword) {
        try {
            SearchResponse<SongDocument> response = esClient.search(s -> s
                            .index("songs")
                            .query(q -> q
                                    .multiMatch(mm -> mm
                                            .query(keyword)
                                            .fields("title^3", "artist^2", "genre", "moodTags")
                                            .operator(Operator.Or)
                                            .type(TextQueryType.BestFields)
                                    )
                            )
                            .size(20),
                    SongDocument.class
            );

            return response.hits().hits().stream()
                    .map(hit -> toDTO(hit.source()))
                    .collect(Collectors.toList());

        } catch (IOException e) {
            log.error("ES 搜索失败: keyword={}", keyword, e);
            return List.of();
        }
    }

    private SongDTO toDTO(SongDocument doc) {
        return SongDTO.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .artist(doc.getArtist())
                .genre(doc.getGenre())
                .fileUrl(doc.getFileUrl())
                .liked(doc.getLiked())
                .moodTags(doc.getMoodTags())
                .build();
    }
}
