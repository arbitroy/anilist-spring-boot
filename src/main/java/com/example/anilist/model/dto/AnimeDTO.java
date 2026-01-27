package com.example.anilist.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Data  // Lombok: generates getters, setters, equals, hashCode, toString
@Builder  // Lombok: generates a builder for creating objects
@NoArgsConstructor  // Lombok: generates no-arg constructor
@AllArgsConstructor  // Lombok: generates all-arg constructor
public class AnimeDTO implements Serializable {

    private static final long serialVersionUID = 1L; 

    private Integer id;
    private String romaji;
    private String english;
    private String native_;  // "native" is reserved, so native_
    
    private String description;
    private Integer episodes;
    private String status;
    private Float averageScore;
    private Integer popularity;
    private String coverImage;
    private String bannerImage;
    
    private LocalDate startDate;
    private LocalDate endDate;
    
    private String season;
    private Integer seasonYear;
    
    private List<String> genres;
    private List<String> studios;
    private String source;
    private String format;
    
    // Helper method
    public String getDisplayTitle() {
        return english != null ? english : romaji;
    }
}