package com.heartify.aiservice.service;

import com.heartify.aiservice.exception.AiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Client service for LLM API
 * Generates medical explanations for ECG analysis results
 */
@Service
public class LLMClient {

    private static final Logger logger = LoggerFactory.getLogger(LLMClient.class);

    private final WebClient webClient;
    
    @Value("${ai.model.llm-api-key}")
    private String apiKey;
    
    @Value("${ai.model.timeout}")
    private int timeout;

    public LLMClient(@Value("${ai.model.llm-api-url}") String llmApiUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(llmApiUrl)
                .build();
        logger.info("LLMClient initialized with URL: {}", llmApiUrl);
    }

    /**
     * Generate medical explanation using LLM
     * 
     * @param diagnosis ECG diagnosis from DL model
     * @param probability Confidence probability
     * @param features Physiological features extracted from ECG
     * @return Map containing explanation response
     */
    public Map<String, Object> generateExplanation(String diagnosis, Double probability, Map<String, Object> features) {
        try {
            logger.info("Calling LLM API for explanation generation");

            // Build the prompt
            String prompt = buildPrompt(diagnosis, probability, features);
            logger.debug("Generated prompt: {}", prompt);

            // Prepare request body for LLM API
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("prompt", prompt);
            
            // Add generation parameters
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("temperature", 0.7);
            parameters.put("max_tokens", 1024);
            parameters.put("top_p", 0.95);
            requestBody.put("parameters", parameters);

            // Call LLM API
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .header("x-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                logger.error("LLM API error: {}", errorBody);
                                return Mono.error(new AiServiceException(
                                    "LLM API returned error: " + errorBody
                                ));
                            })
                    )
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            if (response == null) {
                throw new AiServiceException("LLM API returned null response");
            }

            logger.info("LLM explanation generated successfully");
            
            // Extract explanation from response
            @SuppressWarnings("unchecked")
            Map<String, Object> explanationContent = (Map<String, Object>) response.get("explanation");

            // Build result
            Map<String, Object> result = new HashMap<>();
            result.put("llm_model_version", response.getOrDefault("model_version", 1));
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
        
        prompt.append("**Important Notes**:\n");
        prompt.append("1. Use professional but easy-to-understand English\n");
        prompt.append("2. Avoid alarming the patient unnecessarily\n");
        prompt.append("3. Always emphasize the need to consult a doctor for accurate diagnosis\n");
        prompt.append("4. If abnormal signs are detected, recommend seeing a doctor immediately\n");
        prompt.append("5. Return EXACTLY the JSON format as requested, without any additional text\n");
        
        return prompt.toString();
    }
}
