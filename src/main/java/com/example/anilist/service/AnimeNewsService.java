package com.example.anilist.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

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
    public List<Map<String, Object>> fetchANNNews() {
        try {
            String url = "https://api.rss2json.com/v1/api.json" +
                "?rss_url=https://www.animenewsnetwork.com/news/rss.xml";
            
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            
            List<Map<String, Object>> news = new ArrayList<>();
            root.get("items").forEach(item -> {
                Map<String, Object> newsItem = new HashMap<>();
                newsItem.put("title", item.get("title").asText());
                newsItem.put("link", item.get("link").asText());
                newsItem.put("description", item.get("description").asText());
                newsItem.put("pubDate", item.get("pubDate").asText());
                newsItem.put("source", "Anime News Network");
                newsItem.put("image", item.get("image").asText());
                news.add(newsItem);
            });
            
            log.info("✓ Fetched {} ANN news items", news.size());
            return news;
        } catch (Exception e) {
            log.error("Failed to fetch ANN news: {}", e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Fetch from Reddit r/anime
     */
    public List<Map<String, Object>> fetchRedditNews() {
        try {
            String url = "https://www.reddit.com/r/anime/new.json?limit=10";
            
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            
            List<Map<String, Object>> news = new ArrayList<>();
            root.get("data").get("children").forEach(child -> {
                JsonNode data = child.get("data");
                Map<String, Object> newsItem = new HashMap<>();
                newsItem.put("title", data.get("title").asText());
                newsItem.put("author", data.get("author").asText());
                newsItem.put("url", data.get("url").asText());
                newsItem.put("createdUtc", data.get("created_utc").asLong());
                newsItem.put("upvotes", data.get("ups").asInt());
                newsItem.put("comments", data.get("num_comments").asInt());
                newsItem.put("source", "Reddit r/anime");
                news.add(newsItem);
            });
            
            log.info("✓ Fetched {} Reddit posts", news.size());
            return news;
        } catch (Exception e) {
            log.error("Failed to fetch Reddit news: {}", e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Get combined news from both sources
     */
    public List<Map<String, Object>> getCombinedNews() {
        List<Map<String, Object>> allNews = new ArrayList<>();
        allNews.addAll(fetchANNNews());
        allNews.addAll(fetchRedditNews());
        
        // Sort by date (most recent first)
        allNews.sort((a, b) -> {
            long dateA = Long.parseLong(a.get("pubDate").toString());
            long dateB = Long.parseLong(b.get("pubDate").toString());
            return Long.compare(dateB, dateA);
        });
        
        return allNews;
    }
}