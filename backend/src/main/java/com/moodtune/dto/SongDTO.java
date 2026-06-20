package com.moodtune.dto;

import lombok.Data;

@Data
public class SongDTO {
    private Long id;
    private String title;
    private String artist;
    private String genre;
    private String fileUrl;
    private Boolean liked;
}
