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
     * Build an optimized prompt for LLM using SOTA techniques:
     * - Chain-of-Thought (CoT) reasoning
     * - Few-Shot learning with Vietnamese examples
     * - Contextual relevance with patient-specific features
     * Enhanced with RAG context from medical knowledge base
     * 
     * @param diagnosis ECG diagnosis
     * @param probability Confidence probability
     * @param features Physiological features
     * @param retrievedContext Relevant context from RAG knowledge base
     */
    private String buildPrompt(String diagnosis, Double probability, Map<String, Object> features, String retrievedContext) {
        StringBuilder prompt = new StringBuilder();
        
        // ========== SYSTEM ROLE & INSTRUCTIONS ==========
        prompt.append("You are an expert cardiologist AI assistant specializing in patient education. ");
        prompt.append("Your role is to translate complex ECG analysis into warm, empathetic, and easily understandable explanations ");
        prompt.append("for patients with ZERO medical knowledge, in Vietnamese language.\n\n");
        
        // ========== RAG CONTEXT ==========
        if (retrievedContext != null && !retrievedContext.isBlank()) {
            prompt.append("## Medical Reference Knowledge:\n");
            prompt.append("Use the following authoritative medical information to ensure clinical accuracy:\n\n");
            prompt.append(retrievedContext);
            prompt.append("\n\n---\n\n");
        }
        
        // ========== CHAIN-OF-THOUGHT INSTRUCTIONS ==========
        prompt.append("## Analysis Framework (Chain-of-Thought):\n\n");
        prompt.append("Before generating your final response, you MUST internally reason through these steps:\n\n");
        prompt.append("**Step 1: Feature Analysis**\n");
        prompt.append("- Examine each physiological feature provided below (Heart Rate, HRV, QRS Duration, etc.)\n");
        prompt.append("- Identify which values are normal, borderline, or abnormal\n");
        prompt.append("- Note the clinical significance of each deviation\n\n");
        
        prompt.append("**Step 2: Diagnosis Correlation**\n");
        prompt.append("- Cross-reference the detected diagnosis with the feature values\n");
        prompt.append("- Verify that the features support the diagnosis\n");
        prompt.append("- Consider the confidence level in your explanation\n\n");
        
        prompt.append("**Step 3: Patient Impact Assessment**\n");
        prompt.append("- Determine the real-world meaning for the patient (symptoms, daily life impact)\n");
        prompt.append("- Assess risk level: low (routine monitoring), medium (needs attention), high (urgent care)\n");
        prompt.append("- Decide on appropriate next steps\n\n");
        
        prompt.append("**Step 4: Tone Calibration**\n");
        prompt.append("- Choose a tone that matches the severity: reassuring for benign findings, serious but calm for concerning findings\n");
        prompt.append("- Ensure language is at a 6th-grade reading level (Vietnamese)\n");
        prompt.append("- Use analogies and everyday comparisons, NOT medical jargon\n\n");
        
        prompt.append("---\n\n");
        
        // ========== FEW-SHOT EXAMPLES ==========
        prompt.append("## Few-Shot Examples:\n\n");
        prompt.append("Learn from these examples. Note: Input is technical, but output MUST be in warm, simple Vietnamese.\n\n");
        
        // Example 1: Sinus Tachycardia
        prompt.append("### Example 1:\n");
        prompt.append("**Input:**\n");
        prompt.append("- Diagnosis: Sinus Tachycardia\n");
        prompt.append("- Confidence: 92.5%\n");
        prompt.append("- Heart Rate: 105 bpm\n");
        prompt.append("- HRV (RMSSD): 28 ms\n");
        prompt.append("- QRS Duration: 0.09 s\n\n");
        prompt.append("**Output (Vietnamese):**\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"summary\": \"Kết quả điện tâm đồ cho thấy tim bạn đang đập nhanh hơn bình thường một chút, nhưng đây là điều khá phổ biến và thường không nguy hiểm.\",\n");
        prompt.append("  \"details\": \"Chúng tôi phát hiện nhịp tim của bạn đang ở mức 105 nhịp mỗi phút, trong khi nhịp bình thường của người lớn là khoảng 60-100 nhịp. Hãy tưởng tượng tim bạn như một chiếc đồng hồ đang chạy hơi nhanh một chút - nó vẫn hoạt động đều đặn, chỉ là tần suất cao hơn. Điều này có thể do bạn vừa vận động, lo lắng, uống cà phê, hoặc đơn giản là cơ thể đang cần nhiều oxy hơn. Các chỉ số khác của tim bạn như thời gian tín hiệu điện (QRS) đều nằm trong giới hạn an toàn, điều đó có nghĩa là cấu trúc và hoạt động của tim vẫn tốt.\",\n");
        prompt.append("  \"recommendations\": \"Hãy thử thư giãn, nghỉ ngơi đầy đủ, và tránh những chất kích thích như caffeine hay nicotine trong vài ngày tới. Nếu bạn đang căng thẳng, hãy thử hít thở sâu hoặc đi bộ nhẹ nhàng. Đừng quá lo lắng - tình trạng này thường tự cải thiện.\",\n");
        prompt.append("  \"risk_level\": \"low\",\n");
        prompt.append("  \"next_steps\": \"Theo dõi nhịp tim của bạn trong 1-2 tuần. Nếu bạn cảm thấy hồi hộp mạnh, chóng mặt, hoặc khó thở, hãy đến gặp bác sĩ tim mạch để kiểm tra kỹ hơn. Với kết quả hiện tại, đây chỉ là tình trạng cần theo dõi, chưa cần lo lắng quá mức.\"\n");
        prompt.append("}\n");
        prompt.append("```\n\n");
        
        // Example 2: Atrial Fibrillation
        prompt.append("### Example 2:\n");
        prompt.append("**Input:**\n");
        prompt.append("- Diagnosis: Atrial Fibrillation\n");
        prompt.append("- Confidence: 88.3%\n");
        prompt.append("- Heart Rate: 132 bpm\n");
        prompt.append("- HRV (RMSSD): 45 ms\n");
        prompt.append("- QRS Duration: 0.11 s\n\n");
        prompt.append("**Output (Vietnamese):**\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"summary\": \"Kết quả điện tâm đồ phát hiện nhịp tim của bạn đang hoạt động không đều - tình trạng này gọi là rung nhĩ. Đây là vấn đề cần được bác sĩ theo dõi và điều trị.\",\n");
        prompt.append("  \"details\": \"Hãy tưởng tượng tim bạn có 4 ngăn như 4 căn phòng. Trong tình trạng bình thường, phòng trên cùng (nhĩ) sẽ bóp đều đặn để đẩy máu xuống phòng dưới. Nhưng hiện tại, nhịp đập ở phòng trên đang 'loạn nhịp' - giống như một dàn nhạc mà mỗi nhạc công chơi theo nhịp riêng, không còn hòa hợp nữa. Kết quả là tim bạn đập nhanh (132 nhịp/phút) và không đều. Mặc dù nghe có vẻ đáng lo, nhưng rất nhiều người sống khỏe mạnh với tình trạng này nhờ điều trị đúng cách.\",\n");
        prompt.append("  \"recommendations\": \"Đây KHÔNG phải là tình huống khẩn cấp cần đến cấp cứu ngay lập tức, nhưng bạn cần gặp bác sĩ tim mạch trong vòng vài ngày tới. Bác sĩ có thể kê thuốc giúp điều hòa nhịp tim hoặc ngăn ngừa các biến chứng. Trong lúc chờ đợi, hãy tránh rượu bia, giảm căng thẳng, và không tự ý tập thể dục quá sức.\",\n");
        prompt.append("  \"risk_level\": \"medium\",\n");
        prompt.append("  \"next_steps\": \"Hãy đặt lịch khám bác sĩ tim mạch TRONG TUẦN NÀY. Mang theo kết quả điện tâm đồ này. Nếu bạn đột ngột cảm thấy đau ngực, khó thở nghiêm trọng, hoặc ngất xỉu, hãy gọi cấp cứu 115 ngay. Đối với hầu hết trường hợp rung nhĩ, việc điều trị kịp thời sẽ giúp bạn sống bình thường và khỏe mạnh.\"\n");
        prompt.append("}\n");
        prompt.append("```\n\n");
        
        // Example 3: Normal Sinus Rhythm
        prompt.append("### Example 3:\n");
        prompt.append("**Input:**\n");
        prompt.append("- Diagnosis: Normal Sinus Rhythm\n");
        prompt.append("- Confidence: 96.8%\n");
        prompt.append("- Heart Rate: 72 bpm\n");
        prompt.append("- HRV (RMSSD): 42 ms\n");
        prompt.append("- QRS Duration: 0.08 s\n\n");
        prompt.append("**Output (Vietnamese):**\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"summary\": \"Tin tốt! Kết quả điện tâm đồ của bạn hoàn toàn bình thường. Tim bạn đang hoạt động rất khỏe mạnh và đều đặn.\",\n");
        prompt.append("  \"details\": \"Tất cả các chỉ số mà chúng tôi đo được đều nằm trong khoảng lý tưởng. Nhịp tim của bạn là 72 nhịp mỗi phút - con số 'vàng' cho một trái tim khỏe mạnh. Hệ thống điện của tim (tín hiệu khiến tim co bóp) đang hoạt động trơn tru, giống như hệ thống dây điện trong ngôi nhà được lắp đặt hoàn hảo. Sự biến đổi nhịp tim của bạn (HRV) cũng tốt, cho thấy tim có khả năng thích nghi linh hoạt với các hoạt động khác nhau - đây là dấu hiệu của một trái tim khỏe mạnh!\",\n");
        prompt.append("  \"recommendations\": \"Hãy tiếp tục duy trì lối sống lành mạnh của bạn! Ăn nhiều rau củ, vận động đều đặn (ít nhất 30 phút mỗi ngày), ngủ đủ giấc, và kiểm soát căng thẳng. Nếu bạn hút thuốc, hãy cố gắng bỏ. Đây là những cách tốt nhất để giữ cho tim bạn luôn khỏe mạnh như hiện tại.\",\n");
        prompt.append("  \"risk_level\": \"low\",\n");
        prompt.append("  \"next_steps\": \"Không cần bất kỳ hành động y tế nào. Hãy thực hiện kiểm tra sức khỏe định kỳ hàng năm như bình thường. Nếu trong tương lai bạn có triệu chứng bất thường như đau ngực, khó thở, hoặc tim đập nhanh bất thường, hãy đi khám - nhưng với kết quả hiện tại, bạn hoàn toàn có thể yên tâm!\"\n");
        prompt.append("}\n");
        prompt.append("```\n\n");
        
        prompt.append("---\n\n");
        
        // ========== PATIENT-SPECIFIC DATA ==========
        prompt.append("## Current Patient's ECG Analysis Results:\n\n");
        prompt.append(String.format("**Diagnosis:** %s\n", diagnosis));
        prompt.append(String.format("**Model Confidence:** %.1f%%\n\n", probability * 100));
        
        prompt.append("**Physiological Features Measured:**\n");
        appendFeatureIfPresent(prompt, features, "heart_rate", "Heart Rate", "bpm");
        appendFeatureIfPresent(prompt, features, "hrv_rmssd", "HRV (RMSSD)", "ms");
        appendFeatureIfPresent(prompt, features, "qrs_duration", "QRS Duration", "s");
        appendFeatureIfPresent(prompt, features, "r_amplitude", "R Wave Amplitude", "");
        appendFeatureIfPresent(prompt, features, "signal_energy", "Signal Energy", "");
        appendFeatureIfPresent(prompt, features, "r_peaks_count", "R Peaks Count", "");
        
        prompt.append("\n");
        
        // ========== TASK & OUTPUT FORMAT ==========
        prompt.append("## Your Task:\n\n");
        prompt.append("Using the Chain-of-Thought reasoning process described above, analyze THIS specific patient's data. ");
        prompt.append("Then generate a response in Vietnamese that follows the exact structure shown in the examples.\n\n");
        
        prompt.append("**CRITICAL Requirements:**\n");
        prompt.append("1. Output language: **VIETNAMESE ONLY** (instructions are in English, but your JSON response MUST be Vietnamese)\n");
        prompt.append("2. Tone: Warm, caring, like a kind doctor talking to a family member with no medical background\n");
        prompt.append("3. Vocabulary: Use everyday words, NOT medical jargon (e.g., say 'tim đập nhanh' not 'tachycardia')\n");
        prompt.append("4. Specificity: Reference the ACTUAL feature values from this patient (e.g., 'nhịp tim 105' not 'nhịp tim hơi nhanh')\n");
        prompt.append("5. Analogies: Use relatable comparisons (heart like a clock, electrical system like house wiring, etc.)\n");
        prompt.append("6. Balance: Be honest about concerns but avoid causing panic; be reassuring when appropriate but never dismiss real risks\n\n");
        
        prompt.append("**JSON Response Format:**\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"summary\": \"1-2 câu tóm tắt tình trạng tim mạch bằng tiếng Việt dễ hiểu\",\n");
        prompt.append("  \"details\": \"Giải thích chi tiết các chỉ số ECG, ý nghĩa của chúng, và mối liên hệ với chẩn đoán. Sử dụng ngôn ngữ đời thường, có ví dụ minh họa. Phải đề cập cụ thể đến các giá trị đo được của bệnh nhân này.\",\n");
        prompt.append("  \"recommendations\": \"Khuyến nghị cụ thể cho bệnh nhân (lưu ý: không thay thế lời khuyên của bác sĩ)\",\n");
        prompt.append("  \"risk_level\": \"low/medium/high\",\n");
        prompt.append("  \"next_steps\": \"Các bước tiếp theo bệnh nhân nên thực hiện, thời gian cụ thể nếu cần khám bác sĩ\"\n");
        prompt.append("}\n");
        prompt.append("```\n\n");
        
        prompt.append("**CRITICAL: Return ONLY the JSON object. Do NOT add any text before or after. Do NOT wrap in an array.**");
        
        return prompt.toString();
    }
    
    /**
     * Helper method to append physiological feature to prompt if present
     */
    private void appendFeatureIfPresent(StringBuilder prompt, Map<String, Object> features, 
                                       String key, String label, String unit) {
        if (features.containsKey(key) && features.get(key) != null) {
            String value = String.valueOf(features.get(key));
            if (unit != null && !unit.isEmpty()) {
                prompt.append(String.format("- %s: %s %s\n", label, value, unit));
            } else {
                prompt.append(String.format("- %s: %s\n", label, value));
            }
        }
    }
}
