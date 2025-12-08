package com.heartify.aiservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heartify.aiservice.exception.AiServiceException;
import com.heartify.aiservice.rag.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Client service for LLM API (Google Gemini)
 * Generates medical explanations for ECG analysis results
 * Enhanced with RAG (Retrieval-Augmented Generation) for domain knowledge
 */
@Service
public class LLMClient {

    private static final Logger log = LoggerFactory.getLogger(LLMClient.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final VectorStoreService vectorStoreService;
    
    @Value("${ai.model.llm-api-key}")
    private String apiKey;
    
    @Value("${ai.model.timeout}")
    private int timeout;
    
    @Value("${rag.enabled:true}")
    private boolean ragEnabled;
    
    @Value("${rag.top-k:5}")
    private int topK;

    public LLMClient(@Value("${ai.model.llm-api-url}") String llmApiUrl,
                     ObjectMapper objectMapper,
                     VectorStoreService vectorStoreService) {
        this.webClient = WebClient.builder()
                .baseUrl(llmApiUrl)
                .build();
        this.objectMapper = objectMapper;
        this.vectorStoreService = vectorStoreService;
    }

    /**
     * Generate medical explanation using LLM (Gemini) with RAG
     * 
     * @param diagnosis ECG diagnosis from DL model
     * @param probability Confidence probability
     * @param features Physiological features extracted from ECG
     * @return Map containing explanation response
     */
    public Map<String, Object> generateExplanation(String diagnosis, Double probability, Map<String, Object> features) {
        try {
            // Retrieve relevant medical context using RAG
            String retrievedContext = "";
            if (ragEnabled) {
                retrievedContext = retrieveRelevantContext(diagnosis, features);
                log.debug("Retrieved context for RAG: {} characters", retrievedContext.length());
            }
            
            // Build the prompt with RAG context
            String prompt = buildPrompt(diagnosis, probability, features, retrievedContext);

            // Prepare request body for Gemini API
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            Map<String, String> part = new HashMap<>();
            part.put("text", prompt);
            content.put("parts", List.of(part));
            requestBody.put("contents", List.of(content));
            
            // Add generation config for JSON response
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("topK", 40);
            generationConfig.put("topP", 0.95);
            generationConfig.put("maxOutputTokens", 8192);
            generationConfig.put("responseMimeType", "application/json");
            requestBody.put("generationConfig", generationConfig);

            // Call Gemini API
            String response = webClient.post()
                    .uri(uriBuilder -> uriBuilder.queryParam("key", apiKey).build())
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                return Mono.error(new AiServiceException(
                                    "LLM API returned error: " + errorBody
                                ));
                            })
                    )
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            if (response == null) {
                throw new AiServiceException("LLM API returned null response");
            }

            // Parse Gemini response
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode candidatesNode = rootNode.path("candidates");
            
            if (candidatesNode.isEmpty()) {
                throw new AiServiceException("LLM API returned empty candidates");
            }

            log.debug("Gemini response candidates: {}", candidatesNode.toString());

            String textContent = candidatesNode.get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            // Parse the JSON string directly
            @SuppressWarnings("unchecked")
            Map<String, Object> explanationContent = objectMapper.readValue(textContent, Map.class);

            // Build result
            Map<String, Object> result = new HashMap<>();
            result.put("llm_model_version", 1); // Gemini 2.0 Flash Exp
            result.put("explanation", explanationContent);
            result.put("rag_enabled", ragEnabled);

            return result;

        } catch (Exception e) {
            throw new AiServiceException("Failed to generate explanation from LLM: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieve relevant medical context from vector store using RAG
     * 
     * @param diagnosis The ECG diagnosis
     * @param features Physiological features
     * @return Concatenated relevant context from knowledge base
     */
    private String retrieveRelevantContext(String diagnosis, Map<String, Object> features) {
        try {
            // Build a search query from diagnosis and key features
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("ECG diagnosis: ").append(diagnosis).append(". ");
            
            // Add relevant features to the query for better context retrieval
            if (features.containsKey("heart_rate") && features.get("heart_rate") != null) {
                Object hr = features.get("heart_rate");
                queryBuilder.append("Heart rate: ").append(hr).append(" bpm. ");
            }
            if (features.containsKey("hrv_rmssd") && features.get("hrv_rmssd") != null) {
                queryBuilder.append("Heart rate variability analysis. ");
            }
            if (features.containsKey("qrs_duration") && features.get("qrs_duration") != null) {
                queryBuilder.append("QRS complex duration analysis. ");
            }
            
            queryBuilder.append("Medical explanation cardiac arrhythmia electrocardiogram interpretation.");
            
            String searchQuery = queryBuilder.toString();
            log.debug("RAG search query: {}", searchQuery);
            
            // Search for relevant documents
            List<VectorStoreService.SearchResult> results = vectorStoreService.search(searchQuery, topK);
            
            if (results.isEmpty()) {
                log.debug("No relevant documents found in knowledge base");
                return "";
            }
            
            // Concatenate relevant contexts
            String context = results.stream()
                    .map(VectorStoreService.SearchResult::content)
                    .collect(Collectors.joining("\n\n---\n\n"));
            
            log.info("Retrieved {} relevant documents for context", results.size());
            return context;
            
        } catch (Exception e) {
            log.warn("Failed to retrieve RAG context, proceeding without: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Build a comprehensive prompt for LLM to generate medical explanation
     * Enhanced with RAG context from medical knowledge base
     * 
     * @param diagnosis ECG diagnosis
     * @param probability Confidence probability
     * @param features Physiological features
     * @param retrievedContext Relevant context from RAG knowledge base
     */
    private String buildPrompt(String diagnosis, Double probability, Map<String, Object> features, String retrievedContext) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are a professional medical AI assistant that helps explain ECG (electrocardiogram) analysis results ");
        prompt.append("to patients in a clear, accurate, and reassuring manner.\n\n");
        
        // Include RAG context if available
        if (retrievedContext != null && !retrievedContext.isBlank()) {
            prompt.append("## Reference Medical Knowledge:\n");
            prompt.append("Use the following medical reference information to provide accurate and detailed explanations:\n\n");
            prompt.append(retrievedContext);
            prompt.append("\n\n---\n\n");
        }
        
        prompt.append("## ECG Analysis Results:\n");
        prompt.append(String.format("- **Diagnosis**: %s\n", diagnosis));
        prompt.append(String.format("- **Confidence**: %.1f%%\n", probability * 100));
        
        prompt.append("\n## Physiological Features:\n");
        if (features.containsKey("heart_rate") && features.get("heart_rate") != null) {
            prompt.append(String.format("- Heart Rate: %s bpm\n", features.get("heart_rate")));
        }
        if (features.containsKey("hrv_rmssd") && features.get("hrv_rmssd") != null) {
            prompt.append(String.format("- HRV (RMSSD): %s ms\n", features.get("hrv_rmssd")));
        }
        if (features.containsKey("qrs_duration") && features.get("qrs_duration") != null) {
            prompt.append(String.format("- QRS Duration: %s s\n", features.get("qrs_duration")));
        }
        if (features.containsKey("r_amplitude") && features.get("r_amplitude") != null) {
            prompt.append(String.format("- R Wave Amplitude: %s\n", features.get("r_amplitude")));
        }
        if (features.containsKey("signal_energy") && features.get("signal_energy") != null) {
            prompt.append(String.format("- Signal Energy: %s\n", features.get("signal_energy")));
        }
        if (features.containsKey("r_peaks_count") && features.get("r_peaks_count") != null) {
            prompt.append(String.format("- R Peaks Detected: %s\n", features.get("r_peaks_count")));
        }
        
        prompt.append("\n## Task:\n");
        prompt.append("Please return the result as a JSON object with the following fields:\n\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"summary\": \"A brief summary (1-2 sentences) about the patient's cardiac condition\",\n");
        prompt.append("  \"details\": \"Detailed explanation of the ECG metrics, their significance, ");
        prompt.append("and their relationship to the diagnosis. Use language that is easy for non-medical professionals to understand.\",\n");
        prompt.append("  \"recommendations\": \"Specific recommendations for the patient (note: this does not replace professional medical advice)\",\n");
        prompt.append("  \"risk_level\": \"low/medium/high - Risk assessment based on the results\",\n");
        prompt.append("  \"next_steps\": \"Next steps the patient should take\"\n");
        prompt.append("}\n");
        prompt.append("```\n\n");
        
        prompt.append("**CRITICAL: Return ONLY the JSON object shown above. Do NOT wrap it in an array. Do NOT add any additional text before or after the JSON.**\n\n");
        
        prompt.append("**Important Notes**:\n");
        prompt.append("1. Use professional but easy-to-understand English\n");
        prompt.append("2. Avoid alarming the patient unnecessarily\n");
        prompt.append("3. Always emphasize the need to consult a doctor for accurate diagnosis\n");
        prompt.append("4. If abnormal signs are detected, recommend seeing a doctor immediately\n");
        if (retrievedContext != null && !retrievedContext.isBlank()) {
            prompt.append("5. Base your explanation on the Reference Medical Knowledge provided above when applicable\n");
        }
        
        return prompt.toString();
    }
}
