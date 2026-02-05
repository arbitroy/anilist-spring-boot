package com.example.anilist.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AnimeNewsService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AnimeNewsService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Fetch from RSS2JSON (converts ANN RSS to JSON)
     */
    @Cacheable(value = "annNews", cacheManager = "cacheManager")
    public List<Map<String, Object>> fetchANNNews() {
        try {
            log.info("Starting ANN news fetch...");
            String url = "https://api.rss2json.com/v1/api.json" +
                    "?rss_url=https://www.animenewsnetwork.com/news/rss.xml";

            log.info("Calling RSS2JSON API...");
            String response = restTemplate.getForObject(url, String.class);

            if (response == null) {
                log.error("Got null response from RSS2JSON");
                return List.of();
            }

            log.info("Parsing response...");
            JsonNode root = objectMapper.readTree(response);

            List<Map<String, Object>> news = new ArrayList<>();
            if (root.has("items") && root.get("items").isArray()) {
                root.get("items").forEach(item -> {
                    try {
                        Map<String, Object> newsItem = new HashMap<>();
                        newsItem.put("title", getNodeValue(item, "title"));
                        newsItem.put("link", getNodeValue(item, "link"));
                        newsItem.put("description", getNodeValue(item, "description"));
                        newsItem.put("pubDate", getNodeValue(item, "pubDate"));
                        newsItem.put("source", "Anime News Network");
                        newsItem.put("image", getNodeValue(item, "image"));
                        news.add(newsItem);
                    } catch (Exception e) {
                        log.warn("Error parsing news item: {}", e.getMessage());
                    }
                });
            }

            log.info("✓ Fetched {} ANN news items", news.size());
            return news;
        } catch (Exception e) {
            log.error("Failed to fetch ANN news: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Fetch from Reddit r/anime
     */
    @Cacheable(value = "redditNews", cacheManager = "cacheManager")
    public List<Map<String, Object>> fetchRedditNews() {
        try {
            log.info("Starting Reddit news fetch...");
            String url = "https://www.reddit.com/r/anime/new.json?limit=20";

            log.info("Calling Reddit API...");
            String response = restTemplate.getForObject(url, String.class);

            if (response == null) {
                log.error("Got null response from Reddit");
                return List.of();
            }

            log.info("Parsing response...");
            JsonNode root = objectMapper.readTree(response);

            List<Map<String, Object>> news = new ArrayList<>();
            if (root.has("data") && root.get("data").has("children")) {
                root.get("data").get("children").forEach(child -> {
                    try {
                        JsonNode data = child.get("data");
                        Map<String, Object> newsItem = new HashMap<>();
                        newsItem.put("title", getNodeValue(data, "title"));
                        newsItem.put("author", getNodeValue(data, "author"));
                        newsItem.put("url", getNodeValue(data, "url"));
                        newsItem.put("createdUtc", data.has("created_utc") ? data.get("created_utc").asLong() : 0);
                        newsItem.put("upvotes", data.has("ups") ? data.get("ups").asInt() : 0);
                        newsItem.put("comments", data.has("num_comments") ? data.get("num_comments").asInt() : 0);
                        newsItem.put("source", "Reddit r/anime");
                        news.add(newsItem);
                    } catch (Exception e) {
                        log.warn("Error parsing Reddit post: {}", e.getMessage());
                    }
                });
            }

            log.info("✓ Fetched {} Reddit posts", news.size());
            return news;
        } catch (Exception e) {
            log.error("Failed to fetch Reddit news: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Get combined news from both sources
     */
    @Cacheable(value = "combinedNews", cacheManager = "cacheManager")
    public List<Map<String, Object>> getCombinedNews() {
        try {
            log.info("Fetching combined news...");
            List<Map<String, Object>> allNews = new ArrayList<>();
            allNews.addAll(fetchANNNews());
            allNews.addAll(fetchRedditNews());

            log.info("✓ Got {} total news items", allNews.size());
            return allNews;
        } catch (Exception e) {
            log.error("Failed to fetch combined news: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Helper to safely get node values
     */
    private String getNodeValue(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asText();
        }
        return "";
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