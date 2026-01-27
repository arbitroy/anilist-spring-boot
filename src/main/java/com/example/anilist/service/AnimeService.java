package com.example.anilist.service;

import com.example.anilist.client.AniListGraphQLClient;
import com.example.anilist.client.GraphQLException;
import com.example.anilist.model.dto.AnimeDTO;
import com.example.anilist.model.dto.AnimeSearchResponse;
import com.example.anilist.util.GraphQLQueryLoader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
public class AnimeService {
    private final AniListGraphQLClient graphQLClient;
    private final ObjectMapper objectMapper;
    private final GraphQLQueryLoader queryLoader;

    public AnimeService(AniListGraphQLClient graphQLClient, ObjectMapper objectMapper, GraphQLQueryLoader queryLoader) {
        this.graphQLClient = graphQLClient;
        this.objectMapper = objectMapper;
        this.queryLoader = queryLoader;
    }

    /**
     * Search for anime by title
     * Uses cache to reduce API calls
     */
    @Cacheable(value = "animeSearch", key = "#query + '-' + #page")
    public AnimeSearchResponse searchAnime(String query, Integer page) {
        log.info("Searching for anime: {}", query);

        String graphqlQuery = queryLoader.getQuery("SearchAnime");
        Map<String, Object> variables = Map.of(
                "search", query,
                "page", page != null ? page : 1);

        try {
            JsonNode data = graphQLClient.executeQueryAndExtractData(graphqlQuery, variables);
            return parseSearchResponse(data);
        } catch (Exception e) {
            log.error("Failed to search anime: {}", e.getMessage(), e);
            throw new GraphQLException("Failed to search anime: " + e.getMessage(), e);
        }
    }

    /**
     * Get detailed anime information by ID
     */
    @Cacheable(value = "animeById", key = "#id")
    public AnimeDTO getAnimeById(Integer id) {
        log.info("Fetching anime details for ID: {}", id);
        String graphqlQuery = queryLoader.getQuery("GetAnimeById");

        Map<String, Object> variables = Map.of("id", id);

        try {
            JsonNode data = graphQLClient.executeQueryAndExtractData(graphqlQuery, variables);
            return parseAnimeNode(data.get("Media"));
        } catch (Exception e) {
            log.error("Failed to fetch anime: {}", e.getMessage(), e);
            throw new GraphQLException("Failed to fetch anime: " + e.getMessage(), e);
        }
    }

    /**
     * Get trending anime
     */
    @Cacheable(value = "trendingAnime")
    public AnimeSearchResponse getTrendingAnime(Integer page) {
        log.info("Fetching trending anime");

        String graphqlQuery = queryLoader.getQuery("GetTrendingAnime");

        Map<String, Object> variables = Map.of("page", page != null ? page : 1);

        try {
            JsonNode data = graphQLClient.executeQueryAndExtractData(graphqlQuery, variables);
            return parseSearchResponse(data);
        } catch (Exception e) {
            log.error("Failed to fetch trending anime: {}", e.getMessage(), e);
            throw new GraphQLException("Failed to fetch trending anime: " + e.getMessage(), e);
        }
    }

    /**
     * Get currently airing anime
     */
    @Cacheable(value = "airingAnime")
    public AnimeSearchResponse getAiringAnime(Integer page) {
        log.info("Fetching airing anime");

        String graphqlQuery = queryLoader.getQuery("GetAiringAnime");

        Map<String, Object> variables = Map.of("page", page != null ? page : 1);

        try {
            JsonNode data = graphQLClient.executeQueryAndExtractData(graphqlQuery, variables);
            return parseSearchResponse(data);
        } catch (Exception e) {
            log.error("Failed to fetch airing anime: {}", e.getMessage(), e);
            throw new GraphQLException("Failed to fetch airing anime: " + e.getMessage(), e);
        }
    }

    // Helper methods for parsing

    private AnimeSearchResponse parseSearchResponse(JsonNode pageNode) {
        JsonNode pageInfo = pageNode.get("Page");

        List<AnimeDTO> animes = new ArrayList<>();
        if (pageInfo.has("media")) {
            pageInfo.get("media").forEach(animeNode -> animes.add(parseAnimeNode(animeNode)));
        }

        Integer totalResults = pageInfo
                .get("pageInfo")
                .get("total")
                .asInt();

        return AnimeSearchResponse.builder()
                .media(animes)
                .totalResults(totalResults)
                .build();
    }

    private AnimeDTO parseAnimeNode(JsonNode node) {
        JsonNode title = node.get("title");
        JsonNode startDate = node.get("startDate");
        JsonNode endDate = node.get("endDate");
        JsonNode coverImage = node.get("coverImage");

        List<String> genres = new ArrayList<>();
        if (node.has("genres")) {
            node.get("genres").forEach(g -> genres.add(g.asText()));
        }

        List<String> studios = new ArrayList<>();
        if (node.has("studios") && node.get("studios").has("nodes")) {
            node.get("studios").get("nodes").forEach(s -> studios.add(s.get("name").asText()));
        }

        return AnimeDTO.builder()
                .id(node.get("id").asInt())
                .romaji(title.get("romaji").asText())
                .english(getTextOrNull(title, "english"))
                .native_(getTextOrNull(title, "native"))
                .description(getTextOrNull(node, "description"))
                .episodes(getIntOrNull(node, "episodes"))
                .status(getTextOrNull(node, "status"))
                .averageScore(getFloatOrNull(node, "averageScore"))
                .popularity(getIntOrNull(node, "popularity"))
                .coverImage(getCoverImageUrl(coverImage))
                .bannerImage(getTextOrNull(node, "bannerImage"))
                .startDate(getDateFromNode(startDate))
                .endDate(getDateFromNode(endDate))
                .season(getTextOrNull(node, "season"))
                .seasonYear(getIntOrNull(node, "seasonYear"))
                .genres(genres)
                .studios(studios)
                .source(getTextOrNull(node, "source"))
                .format(getTextOrNull(node, "format"))
                .build();
    }

    private String getTextOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull()
                ? node.get(field).asText()
                : null;
    }

    private Integer getIntOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull()
                ? node.get(field).asInt()
                : null;
    }

    private Float getFloatOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull()
                ? node.get(field).floatValue()
                : null;
    }

    private LocalDate getDateFromNode(JsonNode dateNode) {
        if (dateNode == null || dateNode.isNull()) {
            return null;
        }

        Integer year = getIntOrNull(dateNode, "year");
        Integer month = getIntOrNull(dateNode, "month");
        Integer day = getIntOrNull(dateNode, "day");

        if (year != null && month != null && day != null) {
            return LocalDate.of(year, month, day);
        }

        return null;
    }

    private String getCoverImageUrl(JsonNode coverNode) {
        if (coverNode == null || coverNode.isNull()) {
            return null;
        }
        return getTextOrNull(coverNode, "large");
    }
}