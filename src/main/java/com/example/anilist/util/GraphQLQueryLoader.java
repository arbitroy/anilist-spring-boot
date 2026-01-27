package com.example.anilist.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class GraphQLQueryLoader {
    private final Map<String, String> queriesCache = new HashMap<>();
    
    public GraphQLQueryLoader() {
        try {
            queriesCache.put("SearchAnime", loadQuery("graphql/queries/SearchAnime.graphql"));
            queriesCache.put("GetAnimeById", loadQuery("graphql/queries/GetAnimeById.graphql"));
            queriesCache.put("GetTrendingAnime", loadQuery("graphql/queries/GetTrendingAnime.graphql"));
            queriesCache.put("GetAiringAnime", loadQuery("graphql/queries/GetAiringAnime.graphql"));
            log.info("✓ Loaded {} GraphQL queries", queriesCache.size());
        } catch (IOException e) {
            throw new RuntimeException("Cannot load GraphQL queries", e);
        }
    }
    
    private String loadQuery(String resourcePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        return new String(Files.readAllBytes(Paths.get(resource.getURI())));
    }
    
    public String getQuery(String queryName) {
        String query = queriesCache.get(queryName);
        if (query == null) throw new IllegalArgumentException("Query not found: " + queryName);
        return query;
    }
}