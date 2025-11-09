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
 * Client service for Google Gemini API
 * Generates medical explanations for ECG analysis results
 */
@Service
public class GeminiClient {

    private static final Logger logger = LoggerFactory.getLogger(GeminiClient.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${ai.model.gemini-api-key}")
    private String apiKey;
    
    @Value("${ai.model.timeout}")
    private int timeout;

    public GeminiClient(@Value("${ai.model.gemini-api-url}") String geminiApiUrl,
                        ObjectMapper objectMapper) {
        this.webClient = WebClient.builder()
                .baseUrl(geminiApiUrl)
                .build();
        this.objectMapper = objectMapper;
        logger.info("GeminiClient initialized with URL: {}", geminiApiUrl);
    }

    /**
     * Generate medical explanation using Gemini LLM
     * 
     * @param diagnosis ECG diagnosis from DL model
     * @param probability Confidence probability
     * @param features Physiological features extracted from ECG
     * @return Map containing explanation response
     */
    public Map<String, Object> generateExplanation(String diagnosis, Double probability, Map<String, Object> features) {
        try {
            logger.info("Calling Gemini API for explanation generation");

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
                                logger.error("Gemini API error: {}", errorBody);
                                return Mono.error(new AiServiceException(
                                    "Gemini API returned error: " + errorBody
                                ));
                            })
                    )
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            if (response == null) {
                throw new AiServiceException("Gemini API returned null response");
            }

            // Parse response
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode candidatesNode = rootNode.path("candidates");
            
            if (candidatesNode.isEmpty()) {
                throw new AiServiceException("Gemini API returned empty candidates");
            }

            String textContent = candidatesNode.get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            logger.info("Gemini explanation generated successfully");
            
            // Parse the JSON response from Gemini
            @SuppressWarnings("unchecked")
            Map<String, Object> explanationContent = objectMapper.readValue(textContent, Map.class);

            // Build result
            Map<String, Object> result = new HashMap<>();
            result.put("llm_model_version", 1); // Gemini 1.5 Flash
            result.put("explanation", explanationContent);

            return result;

        } catch (Exception e) {
            logger.error("Error calling Gemini API: {}", e.getMessage(), e);
            throw new AiServiceException("Failed to generate explanation from Gemini: " + e.getMessage(), e);
        }
    }

    /**
     * Build a comprehensive prompt for Gemini to generate medical explanation
     */
    private String buildPrompt(String diagnosis, Double probability, Map<String, Object> features) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Bạn là một trợ lý y tế AI chuyên nghiệp, giúp giải thích kết quả phân tích ECG (điện tâm đồ) ");
        prompt.append("cho bệnh nhân một cách dễ hiểu, chính xác và an tâm.\n\n");
        
        prompt.append("## Kết quả phân tích ECG:\n");
        prompt.append(String.format("- **Chẩn đoán**: %s\n", diagnosis));
        prompt.append(String.format("- **Độ tin cậy**: %.1f%%\n", probability * 100));
        
        prompt.append("\n## Các chỉ số sinh lý:\n");
        if (features.containsKey("heart_rate") && features.get("heart_rate") != null) {
            prompt.append(String.format("- Nhịp tim: %s bpm\n", features.get("heart_rate")));
        }
        if (features.containsKey("hrv_rmssd") && features.get("hrv_rmssd") != null) {
            prompt.append(String.format("- HRV (RMSSD): %s ms\n", features.get("hrv_rmssd")));
        }
        if (features.containsKey("qrs_duration") && features.get("qrs_duration") != null) {
            prompt.append(String.format("- Thời gian QRS: %s s\n", features.get("qrs_duration")));
        }
        if (features.containsKey("r_amplitude") && features.get("r_amplitude") != null) {
            prompt.append(String.format("- Biên độ sóng R: %s\n", features.get("r_amplitude")));
        }
        if (features.containsKey("signal_energy") && features.get("signal_energy") != null) {
            prompt.append(String.format("- Năng lượng tín hiệu: %s\n", features.get("signal_energy")));
        }
        if (features.containsKey("r_peaks_count") && features.get("r_peaks_count") != null) {
            prompt.append(String.format("- Số đỉnh R phát hiện: %s\n", features.get("r_peaks_count")));
        }
        
        prompt.append("\n## Yêu cầu:\n");
        prompt.append("Hãy trả về kết quả dưới dạng JSON object với các trường sau:\n\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"summary\": \"Tóm tắt ngắn gọn (1-2 câu) về tình trạng tim mạch của bệnh nhân\",\n");
        prompt.append("  \"details\": \"Giải thích chi tiết về các chỉ số ECG, ý nghĩa của chúng, ");
        prompt.append("và mối liên hệ với chẩn đoán. Sử dụng ngôn ngữ dễ hiểu cho người không chuyên.\",\n");
        prompt.append("  \"recommendations\": \"Các khuyến nghị cụ thể cho bệnh nhân (lưu ý: không thay thế tư vấn y tế chuyên nghiệp)\",\n");
        prompt.append("  \"risk_level\": \"low/medium/high - Đánh giá mức độ rủi ro dựa trên kết quả\",\n");
        prompt.append("  \"next_steps\": \"Các bước tiếp theo bệnh nhân nên thực hiện\"\n");
        prompt.append("}\n");
        prompt.append("```\n\n");
        
        prompt.append("**Lưu ý quan trọng**:\n");
        prompt.append("1. Sử dụng tiếng Việt chuyên nghiệp nhưng dễ hiểu\n");
        prompt.append("2. Tránh gây hoảng sợ cho bệnh nhân\n");
        prompt.append("3. Luôn nhấn mạnh cần tham khảo ý kiến bác sĩ để có chẩn đoán chính xác\n");
        prompt.append("4. Nếu phát hiện dấu hiệu bất thường, khuyến nghị gặp bác sĩ ngay\n");
        prompt.append("5. Trả về CHÍNH XÁC format JSON như yêu cầu, không thêm text nào khác\n");
        
        return prompt.toString();
    }
}
