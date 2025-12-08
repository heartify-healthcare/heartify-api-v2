package com.heartify.aiservice.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heartify.aiservice.exception.AiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client service for Google Gemini Embedding API
 * Converts text into vector embeddings for semantic search
 */
@Service
public class EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingClient.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${ai.model.embedding-api-key}")
    private String apiKey;
    
    @Value("${ai.model.timeout}")
    private int timeout;

    public EmbeddingClient(@Value("${ai.model.embedding-api-url}") String embeddingApiUrl,
                           ObjectMapper objectMapper) {
        this.webClient = WebClient.builder()
                .baseUrl(embeddingApiUrl)
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Generate embedding vector for a single text
     * 
     * @param text The text to convert to embedding
     * @return List of Float values representing the embedding vector
     */
    public List<Float> generateEmbedding(String text) {
        try {
            // Prepare request body for Gemini Embedding API
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            Map<String, String> part = new HashMap<>();
            
            part.put("text", text);
            content.put("parts", List.of(part));
            requestBody.put("content", content);
            
            // Optional: specify task type for better embeddings
            requestBody.put("taskType", "RETRIEVAL_DOCUMENT");

            // Call Gemini Embedding API
            String response = webClient.post()
                    .uri(uriBuilder -> uriBuilder.queryParam("key", apiKey).build())
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                            .flatMap(errorBody -> Mono.error(new AiServiceException(
                                "Embedding API returned error: " + errorBody
                            )))
                    )
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            if (response == null) {
                throw new AiServiceException("Embedding API returned null response");
            }

            // Parse response to extract embedding values
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode embeddingNode = rootNode.path("embedding").path("values");
            
            if (embeddingNode.isMissingNode() || !embeddingNode.isArray()) {
                throw new AiServiceException("Invalid embedding response format");
            }

            List<Float> embedding = new ArrayList<>();
            for (JsonNode value : embeddingNode) {
                embedding.add(value.floatValue());
            }

            log.debug("Generated embedding with {} dimensions", embedding.size());
            return embedding;

        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AiServiceException("Failed to generate embedding: " + e.getMessage(), e);
        }
    }

    /**
     * Generate embedding vector for a query (uses RETRIEVAL_QUERY task type)
     * 
     * @param queryText The query text to convert to embedding
     * @return List of Float values representing the embedding vector
     */
    public List<Float> generateQueryEmbedding(String queryText) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            Map<String, String> part = new HashMap<>();
            
            part.put("text", queryText);
            content.put("parts", List.of(part));
            requestBody.put("content", content);
            requestBody.put("taskType", "RETRIEVAL_QUERY");

            String response = webClient.post()
                    .uri(uriBuilder -> uriBuilder.queryParam("key", apiKey).build())
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                            .flatMap(errorBody -> Mono.error(new AiServiceException(
                                "Embedding API returned error: " + errorBody
                            )))
                    )
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            if (response == null) {
                throw new AiServiceException("Embedding API returned null response");
            }

            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode embeddingNode = rootNode.path("embedding").path("values");
            
            if (embeddingNode.isMissingNode() || !embeddingNode.isArray()) {
                throw new AiServiceException("Invalid embedding response format");
            }

            List<Float> embedding = new ArrayList<>();
            for (JsonNode value : embeddingNode) {
                embedding.add(value.floatValue());
            }

            log.debug("Generated query embedding with {} dimensions", embedding.size());
            return embedding;

        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AiServiceException("Failed to generate query embedding: " + e.getMessage(), e);
        }
    }

    /**
     * Generate embeddings for multiple texts in batch
     * 
     * @param texts List of texts to convert to embeddings
     * @return List of embedding vectors
     */
    public List<List<Float>> generateEmbeddings(List<String> texts) {
        List<List<Float>> embeddings = new ArrayList<>();
        for (String text : texts) {
            embeddings.add(generateEmbedding(text));
        }
        return embeddings;
    }
}
