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
 * Client service for ECG Denoising API
 * Calls the heartify-denoised-model denoising endpoint
 */
@Service
public class DenoisingClient {

    private final WebClient webClient;
    
    @Value("${ai.model.denoising-api-key}")
    private String apiKey;
    
    @Value("${ai.model.timeout}")
    private int timeout;

    public DenoisingClient(@Value("${ai.model.denoising-api-url}") String denoisingApiUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(denoisingApiUrl)
                .build();
    }

    /**
     * Call Denoising Model to clean ECG signal
     * 
     * @param ecgSignal Array of 1300 float values representing raw ECG signal
     * @return Map containing denoised results (modelVersion, denoised_signal)
     */
    public Map<String, Object> denoise(List<Double> ecgSignal) {
        try {
            // Validate input length
            if (ecgSignal == null || ecgSignal.size() != 1300) {
                throw new AiServiceException(
                    "ECG signal must have exactly 1300 values (130Hz sampling rate with 10-second duration)"
                );
            }

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
                                    "Denoising API returned error: " + errorBody
                                ));
                            })
                    )
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            if (response == null) {
                throw new AiServiceException("Denoising API returned null response");
            }

            // Convert response to expected format
            Map<String, Object> result = new HashMap<>();
            result.put("model_version", response.get("modelVersion"));
            result.put("denoised_signal", response.get("denoised_signal"));

            return result;

        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AiServiceException("Failed to denoise ECG signal: " + e.getMessage(), e);
        }
    }
}
