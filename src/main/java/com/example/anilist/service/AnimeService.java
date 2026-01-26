package com.example.anilist.service;

import com.example.anilist.client.AniListGraphQLClient;
import com.example.anilist.client.GraphQLException;
import com.example.anilist.model.dto.AnimeDTO;
import com.example.anilist.model.dto.AnimeSearchResponse;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service  // Spring will manage this bean
public class AnimeService {
    private final AniListGraphQLClient graphQLClient;

    public AnimeService(AniListGraphQLClient graphQLClient) {
        this.graphQLClient = graphQLClient;
    }

    /**
     * Search for anime by title
     * Results are cached for 1 hour
     */
    @Cacheable(value = "animeSearch", key = "#query + '-' + #page")
    public AnimeSearchResponse searchAnime(String query, Integer page) {
        log.info("Searching for anime: {}", query);
        
        // Define GraphQL query
        String graphqlQuery = """
            query SearchAnime($search: String!, $page: Int) {
              Page(page: $page, perPage: 10) {
                pageInfo {
                  total
                }
                media(search: $search, type: ANIME) {
                  id
                  title {
                    romaji
                    english
                    native
                  }
                  episodes
                  status
                  averageScore
                  coverImage {
                    large
                  }
                  genres
                }
              }
            }
            """;
        
        // Set variables
        Map<String, Object> variables = Map.of(
            "search", query,
            "page", page != null ? page : 1
        );
        
        try {
            // Execute query
            JsonNode data = graphQLClient.executeQueryAndExtractData(graphqlQuery, variables);
            
            // Parse response into AnimeSearchResponse
            return parseSearchResponse(data);
        } catch (Exception e) {
            log.error("Failed to search anime: {}", e.getMessage());
            throw new GraphQLException("Search failed: " + e.getMessage());
        }
    }

    /**
     * Get anime by ID
     */
    @Cacheable(value = "animeById", key = "#id")
    public AnimeDTO getAnimeById(Integer id) {
        log.info("Fetching anime ID: {}", id);
        
        String graphqlQuery = """
            query ($id: Int!) {
              Media(id: $id) {
                id
                title {
                  romaji
                  english
                  native
                }
                episodes
                status
                averageScore
                coverImage {
                  large
                }
                genres
              }
            }
            """;
        
        Map<String, Object> variables = Map.of("id", id);
        
        try {
            JsonNode data = graphQLClient.executeQueryAndExtractData(graphqlQuery, variables);
            return parseAnimeNode(data.get("Media"));
        } catch (Exception e) {
            log.error("Failed to fetch anime: {}", e.getMessage());
            throw new GraphQLException("Fetch failed: " + e.getMessage());
        }
    }

    // ===== Helper Methods =====

    private AnimeSearchResponse parseSearchResponse(JsonNode pageNode) {
        JsonNode pageInfo = pageNode.get("Page");
        
        List<AnimeDTO> animes = new ArrayList<>();
        if (pageInfo.has("media")) {
            pageInfo.get("media").forEach(animeNode -> 
                animes.add(parseAnimeNode(animeNode))
            );
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
        JsonNode coverImage = node.get("coverImage");
        
        List<String> genres = new ArrayList<>();
        if (node.has("genres")) {
            node.get("genres").forEach(g -> genres.add(g.asText()));
        }
        
        return AnimeDTO.builder()
            .id(node.get("id").asInt())
            .romaji(title.get("romaji").asText())
            .english(getTextOrNull(title, "english"))
            .native_(getTextOrNull(title, "native"))
            .episodes(getIntOrNull(node, "episodes"))
            .status(getTextOrNull(node, "status"))
            .averageScore(getFloatOrNull(node, "averageScore"))
            .coverImage(getCoverImageUrl(coverImage))
            .genres(genres)
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

    private String getCoverImageUrl(JsonNode coverNode) {
        if (coverNode == null || coverNode.isNull()) {
            return null;
        }
        return getTextOrNull(coverNode, "large");
    }
}