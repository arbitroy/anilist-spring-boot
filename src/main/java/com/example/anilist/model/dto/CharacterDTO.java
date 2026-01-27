package com.example.anilist.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterDTO implements Serializable{
    private static final long serialVersionUID = 1L;
    private Integer id;
    private String name;
    private String imageUrl;
    private String description;
    private List<String> roles;
    private Integer popularity;
}