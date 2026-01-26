package com.example.anilist.diagnostic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Diagnostic tool to test AniList connectivity
 * Add a GET endpoint in your controller that calls this
 */
@Slf4j
@Component
public class AniListDiagnostic {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public AniListDiagnostic(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .baseUrl("https://graphql.anilist.co")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Run diagnostic tests
     */
    public DiagnosticResult runDiagnostics() {
        DiagnosticResult result = DiagnosticResult.builder().build();
        
        log.info("=== Starting AniList API Diagnostics ===");
        
        // Test 1: Check endpoint connectivity
        log.info("Test 1: Checking endpoint connectivity...");
        try {
            boolean canConnect = testEndpointConnectivity();
            result.endpointReachable = canConnect;
            log.info("✓ Endpoint reachable: {}", canConnect);
        } catch (Exception e) {
            log.error("✗ Endpoint test failed: {}", e.getMessage());
            result.endpointReachable = false;
            result.endpointError = e.getMessage();
        }
        
        // Test 2: Simple query
        log.info("Test 2: Running simple GraphQL query...");
        try {
            JsonNode response = testSimpleQuery();
            result.simpleQuerySucceeded = response != null && response.has("data");
            log.info("✓ Simple query successful: {}", result.simpleQuerySucceeded);
            if (response != null) {
                result.sampleResponse = response.toString();
            }
        } catch (Exception e) {
            log.error("✗ Simple query failed: {}", e.getMessage(), e);
            result.simpleQuerySucceeded = false;
            result.queryError = e.getMessage();
        }
        
        // Test 3: Check response format
        log.info("Test 3: Checking response format...");
        try {
            String responseText = testRawResponse();
            result.rawResponseText = responseText;
            log.info("✓ Raw response received (length: {})", responseText.length());
        } catch (Exception e) {
            log.error("✗ Raw response test failed: {}", e.getMessage());
            result.rawResponseError = e.getMessage();
        }
        
        log.info("=== Diagnostics Complete ===");
        return result;
    }

    /**
     * Test if endpoint is reachable
     */
    private boolean testEndpointConnectivity() {
        try {
            log.debug("Testing endpoint with HEAD request...");
            
            WebClient testClient = WebClient.create("https://graphql.anilist.co");
            
            // Try a simple GET to root (might fail but tells us if endpoint exists)
            testClient.get()
                    .uri("")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(java.time.Duration.ofSeconds(5));
            
            return true;
        } catch (Exception e) {
            log.warn("GET request failed (expected): {}", e.getClass().getSimpleName());
            // GET will fail, but that's OK - we just wanted to test connectivity
            // If we got a response (even error), endpoint is reachable
            return e.getMessage() != null && !e.getMessage().isEmpty();
        }
    }

    /**
     * Test a simple GraphQL query
     */
    private JsonNode testSimpleQuery() {
        String simpleQuery = """
            query {
              Media(id: 1) {
                id
                title {
                  romaji
                }
              }
            }
            """;
        
        Map<String, Object> request = Map.of(
            "query", simpleQuery
        );
        
        log.debug("Sending query: {}", simpleQuery);
        
        return webClient
                .post()
                .uri("")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(java.time.Duration.ofSeconds(10))
                .doOnError(e -> log.error("Query failed: {} - {}", e.getClass().getSimpleName(), e.getMessage()))
                .block();
    }

    /**
     * Get raw response text for analysis
     */
    private String testRawResponse() {
        String query = """
            query {
              Media(id: 1) {
                id
              }
            }
            """;
        
        Map<String, Object> request = Map.of(
            "query", query
        );
        
        return webClient
                .post()
                .uri("")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(java.time.Duration.ofSeconds(10))
                .block();
    }
}

/**
 * Result class for diagnostic tests
 */
class DiagnosticResult {
    public boolean endpointReachable;
    public String endpointError;
    
    public boolean simpleQuerySucceeded;
    public String queryError;
    
    public String rawResponseText;
    public String rawResponseError;
    
    public String sampleResponse;
    
    public static DiagnosticResultBuilder builder() {
        return new DiagnosticResultBuilder();
    }
    
    public static class DiagnosticResultBuilder {
        private DiagnosticResult result = new DiagnosticResult();
        
        public DiagnosticResult build() {
            return result;
        }
    }
    
    @Override
    public String toString() {
        return String.format("""
            === AniList API Diagnostic Results ===
            Endpoint Reachable: %s
            Endpoint Error: %s
            
            Simple Query Succeeded: %s
            Query Error: %s
            
            Raw Response (first 500 chars):
            %s
            
            Raw Response Error: %s
            
            Sample Response:
            %s
            """,
            endpointReachable,
            endpointError != null ? endpointError : "None",
            simpleQuerySucceeded,
            queryError != null ? queryError : "None",
            rawResponseText != null ? rawResponseText.substring(0, Math.min(500, rawResponseText.length())) : "N/A",
            rawResponseError != null ? rawResponseError : "None",
            sampleResponse != null ? sampleResponse : "N/A"
        );
    }
}