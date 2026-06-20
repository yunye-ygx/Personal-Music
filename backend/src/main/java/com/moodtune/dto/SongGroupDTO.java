package com.moodtune.dto;

import lombok.Data;
import java.util.List;

@Data
public class SongGroupDTO {
    private Long id;
    private String name;
    private String source;
    private List<SongDTO> songs;
}
