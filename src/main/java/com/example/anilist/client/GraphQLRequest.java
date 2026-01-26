package com.example.anilist.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphQLRequest {
    private String query;  // The GraphQL query string
    private Map<String, Object> variables;  // Variables for the query
    
    @JsonProperty("operationName")
    private String operationName;
}