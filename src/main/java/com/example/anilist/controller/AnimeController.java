package com.example.anilist.controller;

import com.example.anilist.model.dto.AnimeDTO;
import com.example.anilist.model.dto.AnimeSearchResponse;
import com.example.anilist.service.AnimeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/anime")
@CrossOrigin(origins = "*")
public class AnimeController {
    private final AnimeService animeService;

    public AnimeController(AnimeService animeService) {
        this.animeService = animeService;
    }

    /**
     * Search anime by title
     * GET /api/anime/search?query=naruto&page=1
     */
    @GetMapping("/search")
    public ResponseEntity<AnimeSearchResponse> searchAnime(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") Integer page) {
        
        log.info("Received search request for: {}", query);
        
        AnimeSearchResponse response = animeService.searchAnime(query, page);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get anime by ID
     * GET /api/anime/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<AnimeDTO> getAnimeById(@PathVariable Integer id) {
        log.info("Received request for anime ID: {}", id);
        
        AnimeDTO anime = animeService.getAnimeById(id);
        
        return ResponseEntity.ok(anime);
    }

    /**
     * Get trending anime
     * GET /api/anime/trending?page=1
     */
    @GetMapping("/trending")
    public ResponseEntity<AnimeSearchResponse> getTrendingAnime(
            @RequestParam(defaultValue = "1") Integer page) {
        
        log.info("Received trending anime request");
        
        AnimeSearchResponse response = animeService.getTrendingAnime(page);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get currently airing anime
     * GET /api/anime/airing?page=1
     */
    @GetMapping("/airing")
    public ResponseEntity<AnimeSearchResponse> getAiringAnime(
            @RequestParam(defaultValue = "1") Integer page) {
        
        log.info("Received airing anime request");
        
        AnimeSearchResponse response = animeService.getAiringAnime(page);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AniList GraphQL Client is running!");
    }
}