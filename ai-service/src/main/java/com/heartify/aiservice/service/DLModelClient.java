package com.heartify.aiservice.service;

import com.heartify.aiservice.exception.AiServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client service for Deep Learning Model API
 * Calls the heartify-dl-model prediction endpoint
 */
@Service
public class DLModelClient {

    private final WebClient webClient;
    
    @Value("${ai.model.prediction-api-key}")
    private String apiKey;
    
    @Value("${ai.model.timeout}")
    private int timeout;

    public DLModelClient(@Value("${ai.model.prediction-api-url}") String predictionApiUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(predictionApiUrl)
                .build();
    }

    /**
     * Call Deep Learning Model to predict ECG signal
     * 
     * @param ecgSignal Array of 1300 float values representing ECG signal
     * @return Map containing prediction results (modelVersion, diagnosis, probability, features)
     */
    public Map<String, Object> predict(List<Double> ecgSignal) {
        try {
            // Prepare request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("ecg_signal", ecgSignal);

            // Call the API
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
                                return Mono.error(new AiServiceException(
                                    "DL Model API returned error: " + errorBody
                                ));
                            })
                    )
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            if (response == null) {
                throw new AiServiceException("DL Model API returned null response");
            }

            // Convert response to expected format
            Map<String, Object> result = new HashMap<>();
            result.put("model_version", response.get("modelVersion"));
            result.put("diagnosis", response.get("diagnosis"));
            result.put("probability", response.get("probability"));
            result.put("features", response.get("features"));
            
            // Extract Base64-encoded ECG image for multimodal AI (transient, not stored in DB)
            String ecgImageBase64 = (String) response.get("ecgImageBase64");
            if (ecgImageBase64 != null && !ecgImageBase64.isEmpty()) {
                result.put("ecg_image_base64", ecgImageBase64);
            }

            return result;

        } catch (Exception e) {
            throw new AiServiceException("Failed to get prediction from DL Model: " + e.getMessage(), e);
        }
    }
}
