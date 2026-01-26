package com.example.anilist.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

/**
 * Fixed GraphQL Client that works around Jackson deserialization issues
 * Instead of deserializing directly to JsonNode, we get String and parse it manually
 */
@Slf4j
@Component
public class AniListGraphQLClient {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${anilist.graphql.timeout-seconds:30}")
    private long timeoutSeconds;

    public AniListGraphQLClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .baseUrl("https://graphql.anilist.co")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Execute a GraphQL query
     * Returns response as JsonNode
     */
    public JsonNode executeQuery(String query, Map<String, Object> variables) {
        log.debug("Executing GraphQL query");
        
        GraphQLRequest request = GraphQLRequest.builder()
                .query(query)
                .variables(variables)
                .build();
        
        try {
            return executeInternal(request)
                    .block();
        } catch (Exception e) {
            log.error("Request failed: {}", e.getMessage(), e);
            throw new GraphQLException("Request failed: " + e.getMessage());
        }
    }

    /**
     * Execute query and extract just the data portion
     */
    public JsonNode executeQueryAndExtractData(String query, Map<String, Object> variables) {
        JsonNode response = executeQuery(query, variables);
        
        if (response == null) {
            throw new GraphQLException("Null response from AniList");
        }
        
        log.debug("Response received successfully");
        
        // Check for GraphQL errors
        if (response.has("errors") && response.get("errors").size() > 0) {
            String errorMsg = response.get("errors").toString();
            log.error("GraphQL errors in response: {}", errorMsg);
            throw new GraphQLException("GraphQL Error: " + errorMsg);
        }
        
        // Return data
        if (response.has("data")) {
            JsonNode data = response.get("data");
            if (data.isNull()) {
                throw new GraphQLException("Data is null in response");
            }
            return data;
        }
        
        throw new GraphQLException("No 'data' field in response");
    }

    /**
     * Internal method that handles the actual request with retry logic
     * Gets response as String and parses it manually to avoid Jackson issues
     */
    private Mono<JsonNode> executeInternal(GraphQLRequest request) {
        log.debug("Making request to AniList API");
        
        return webClient
                .post()
                .uri("")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)  // Get as String instead of JsonNode
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .map(responseString -> {
                    try {
                        // Manually parse the String to JsonNode
                        return objectMapper.readTree(responseString);
                    } catch (Exception e) {
                        log.error("Failed to parse response: {}", e.getMessage());
                        throw new GraphQLException("Failed to parse JSON response: " + e.getMessage());
                    }
                })
                .doOnSuccess(response -> log.debug("Received response successfully"))
                .doOnError(error -> log.warn("Request error (will retry): {}", error.getClass().getSimpleName()))
                .retryWhen(
                    Retry.backoff(3, Duration.ofMillis(500))
                        .maxBackoff(Duration.ofSeconds(2))
                        .jitter(0.5)
                        .doBeforeRetry(signal -> 
                            log.warn("Retry attempt {} after error: {}", 
                                signal.totalRetries() + 1, 
                                signal.failure().getMessage())
                        )
                );
    }
}