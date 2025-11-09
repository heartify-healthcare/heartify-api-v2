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
import java.util.List;
import java.util.Map;

/**
 * Client service for Deep Learning Model API
 * Calls the heartify-dl-model prediction endpoint
 */
@Service
public class DLModelClient {

    private static final Logger logger = LoggerFactory.getLogger(DLModelClient.class);

    private final WebClient webClient;
    
    @Value("${ai.model.prediction-api-key}")
    private String apiKey;
    
    @Value("${ai.model.timeout}")
    private int timeout;

    public DLModelClient(@Value("${ai.model.prediction-api-url}") String predictionApiUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(predictionApiUrl)
                .build();
        logger.info("DLModelClient initialized with URL: {}", predictionApiUrl);
    }

    /**
     * Call Deep Learning Model to predict ECG signal
     * 
     * @param ecgSignal Array of 130 float values representing ECG signal
     * @return Map containing prediction results (modelVersion, diagnosis, probability, features)
     */
    public Map<String, Object> predict(List<Double> ecgSignal) {
        try {
            logger.info("Calling DL Model API for ECG prediction");

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
                                logger.error("DL Model API error: {}", errorBody);
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

            logger.info("DL Model prediction successful: {}", response.get("diagnosis"));
            
            // Convert response to expected format
            Map<String, Object> result = new HashMap<>();
            result.put("model_version", response.get("modelVersion"));
            result.put("diagnosis", response.get("diagnosis"));
            result.put("probability", response.get("probability"));
            result.put("features", response.get("features"));

            return result;

        } catch (Exception e) {
            logger.error("Error calling DL Model API: {}", e.getMessage(), e);
            throw new AiServiceException("Failed to get prediction from DL Model: " + e.getMessage(), e);
        }
    }
}
