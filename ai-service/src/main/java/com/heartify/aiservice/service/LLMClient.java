package com.heartify.aiservice.service;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client service for LLM API (Google Gemini)
 * Generates medical explanations for ECG analysis results
 */
@Service
public class LLMClient {

    private static final Logger logger = LoggerFactory.getLogger(LLMClient.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${ai.model.llm-api-key}")
    private String apiKey;
    
    @Value("${ai.model.timeout}")
    private int timeout;

    public LLMClient(@Value("${ai.model.llm-api-url}") String llmApiUrl,
                     ObjectMapper objectMapper) {
        this.webClient = WebClient.builder()
                .baseUrl(llmApiUrl)
                .build();
        this.objectMapper = objectMapper;
        logger.info("LLMClient initialized with URL: {}", llmApiUrl);
    }

    /**
     * Generate medical explanation using LLM (Gemini)
     * 
     * @param diagnosis ECG diagnosis from DL model
     * @param probability Confidence probability
     * @param features Physiological features extracted from ECG
     * @return Map containing explanation response
     */
    public Map<String, Object> generateExplanation(String diagnosis, Double probability, Map<String, Object> features) {
        try {
            logger.info("Calling LLM API (Gemini) for explanation generation");

            // Build the prompt
            String prompt = buildPrompt(diagnosis, probability, features);
            logger.debug("Generated prompt: {}", prompt);

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
            generationConfig.put("maxOutputTokens", 1024);
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
                                logger.error("LLM API (Gemini) error: {}", errorBody);
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

            logger.debug("Raw Gemini response: {}", response);

            // Parse Gemini response
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode candidatesNode = rootNode.path("candidates");
            
            if (candidatesNode.isEmpty()) {
                logger.error("Empty candidates in Gemini response: {}", response);
                throw new AiServiceException("LLM API returned empty candidates");
            }

            String textContent = candidatesNode.get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            logger.debug("Extracted text content from Gemini: {}", textContent);

            logger.info("LLM explanation generated successfully");
            
            // Parse the JSON response from Gemini
            // The response might be a JSON object or might need cleaning
            String cleanedJson = textContent.trim();
            
            // Remove markdown code block if present
            if (cleanedJson.startsWith("```json")) {
                cleanedJson = cleanedJson.substring(7);
            }
            if (cleanedJson.startsWith("```")) {
                cleanedJson = cleanedJson.substring(3);
            }
            if (cleanedJson.endsWith("```")) {
                cleanedJson = cleanedJson.substring(0, cleanedJson.length() - 3);
            }
            cleanedJson = cleanedJson.trim();
            
            logger.debug("Cleaned JSON response: {}", cleanedJson);
            
            Map<String, Object> explanationContent;
            
            // Try to parse as object first
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> parsedMap = objectMapper.readValue(cleanedJson, Map.class);
                explanationContent = parsedMap;
            } catch (Exception e) {
                // If it's an array, try to get the first element
                logger.warn("Failed to parse as object, attempting to parse as array: {}", e.getMessage());
                try {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> parsedList = objectMapper.readValue(cleanedJson, List.class);
                    if (parsedList != null && !parsedList.isEmpty()) {
                        explanationContent = parsedList.get(0);
                        logger.info("Successfully extracted first element from array response");
                    } else {
                        throw new AiServiceException("LLM returned empty array");
                    }
                } catch (Exception ex) {
                    logger.error("Failed to parse LLM response as both object and array. Raw content: {}", cleanedJson);
                    throw new AiServiceException("Failed to parse LLM response: " + ex.getMessage());
                }
            }

            // Build result
            Map<String, Object> result = new HashMap<>();
            result.put("llm_model_version", 1); // Gemini 2.5 Flash
            result.put("explanation", explanationContent);

            return result;

        } catch (Exception e) {
            logger.error("Error calling LLM API: {}", e.getMessage(), e);
            throw new AiServiceException("Failed to generate explanation from LLM: " + e.getMessage(), e);
        }
    }

    /**
     * Build a comprehensive prompt for LLM to generate medical explanation
     */
    private String buildPrompt(String diagnosis, Double probability, Map<String, Object> features) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are a professional medical AI assistant that helps explain ECG (electrocardiogram) analysis results ");
        prompt.append("to patients in a clear, accurate, and reassuring manner.\n\n");
        
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
        
        return prompt.toString();
    }
}
