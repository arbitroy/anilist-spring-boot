package com.example.anilist.controller;

import com.example.anilist.diagnostic.AniListDiagnostic;
import com.example.anilist.model.dto.AnimeDTO;
import com.example.anilist.model.dto.AnimeSearchResponse;
import com.example.anilist.service.AnimeService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController // This is a REST controller
@RequestMapping("/anime") // Base path: /api/anime
@CrossOrigin(origins = "*") // Allow requests from any origin
public class AnimeController {
    private final AnimeService animeService;

    @Autowired
    private AniListDiagnostic diagnostic;

    public AnimeController(AnimeService animeService) {
        this.animeService = animeService;
    }

    /**
     * Search anime
     * GET /api/anime/search?query=naruto&page=1
     */
    @GetMapping("/search")
    public ResponseEntity<AnimeSearchResponse> searchAnime(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") Integer page) {

        log.info("Search request: {}", query);
        AnimeSearchResponse response = animeService.searchAnime(query, page);
        return ResponseEntity.ok(response);
    }

    /**
     * Get anime by ID
     * GET /api/anime/20
     */
    @GetMapping("/{id}")
    public ResponseEntity<AnimeDTO> getAnimeById(@PathVariable Integer id) {
        log.info("Get anime ID: {}", id);
        AnimeDTO anime = animeService.getAnimeById(id);
        return ResponseEntity.ok(anime);
    }

    /**
     * Health check
     * GET /api/anime/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("API is running!");
    }

    // @GetMapping("/diagnostic")
    // public ResponseEntity<String> runDiagnostic() {
    //     DiagnosticResult result = diagnostic.runDiagnostics();
    //     return ResponseEntity.ok(result.toString());
    // }
}