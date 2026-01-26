package com.example.anilist.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimeSearchResponse {
    private List<AnimeDTO> media;
    private Integer totalResults;
    
    public boolean hasResults() {
        return media != null && !media.isEmpty();
    }
}