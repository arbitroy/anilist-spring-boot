package com.example.anilist.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import com.example.anilist.service.AnimeNewsService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/anime/news")
@CrossOrigin(origins = "*")
public class AnimeNewsController {
    private final AnimeNewsService newsService;

    public AnimeNewsController(AnimeNewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping("/ann")
    public ResponseEntity<List<Map<String, Object>>> getANNNews() {
        log.info("Fetching ANN news");
        return ResponseEntity.ok(newsService.fetchANNNews());
    }

    @GetMapping("/reddit")
    public ResponseEntity<List<Map<String, Object>>> getRedditNews() {
        log.info("Fetching Reddit r/anime news");
        return ResponseEntity.ok(newsService.fetchRedditNews());
    }

    @GetMapping("/combined")
    public ResponseEntity<List<Map<String, Object>>> getCombinedNews() {
        log.info("Fetching combined anime news");
        return ResponseEntity.ok(newsService.getCombinedNews());
    }
}
