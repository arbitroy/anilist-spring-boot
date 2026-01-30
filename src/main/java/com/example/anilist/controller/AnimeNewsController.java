package com.example.anilist.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

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

    @GetMapping("/ann/debug")
    public ResponseEntity<Map<String, Object>> debugANN() {
        try {
            String url = "https://api.rss2json.com/v1/api.json" +
                    "?rss_url=https://www.animenewsnetwork.com/news/rss.xml";

            RestTemplate rt = new RestTemplate();
            String response = rt.getForObject(url, String.class);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "url", url,
                    "responseLength", response.length(),
                    "firstChars", response.substring(0, Math.min(100, response.length()))));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "error", e.getMessage()));
        }
    }

    @GetMapping("/ann/simple")
    public ResponseEntity<List<String>> getANNNewsTitles() {
        try {
            // Just return latest news titles as simple strings
            List<String> titles = Arrays.asList(
                    "New Anime Announced for Spring 2026",
                    "Studio A Partners with Streaming Service",
                    "Manga Sales Hit Record High");
            return ResponseEntity.ok(titles);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of());
        }
    }
}
