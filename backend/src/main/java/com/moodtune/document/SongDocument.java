package com.moodtune.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "songs")
public class SongDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String artist;

    @Field(type = FieldType.Keyword)
    private String genre;

    @Field(type = FieldType.Keyword, index = false)
    private String fileUrl;

    @Field(type = FieldType.Boolean)
    private Boolean liked;

    @Field(type = FieldType.Keyword)
    private List<String> moodTags;
}
