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
        return generateExplanation(diagnosis, probability, features, null);
    }

    /**
     * Generate medical explanation using LLM (Gemini) with RAG and optional ECG image (Multimodal)
     * 
     * @param diagnosis ECG diagnosis from DL model
     * @param probability Confidence probability
     * @param features Physiological features extracted from ECG
     * @param ecgImageBase64 Base64-encoded ECG signal image for multimodal analysis (can be null)
     * @return Map containing explanation response
     */
    public Map<String, Object> generateExplanation(String diagnosis, Double probability, 
                                                    Map<String, Object> features, String ecgImageBase64) {
        try {
            // Retrieve relevant medical context using RAG
            String retrievedContext = "";
            if (ragEnabled) {
                retrievedContext = retrieveRelevantContext(diagnosis, features);
                log.debug("Retrieved context for RAG: {} characters", retrievedContext.length());
            }
            
            // Build the prompt with RAG context
            String prompt = buildPrompt(diagnosis, probability, features, retrievedContext);

            // Prepare request body for Gemini API (supports multimodal with image)
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            
            // Build parts array - text prompt + optional image
            List<Map<String, Object>> parts = new java.util.ArrayList<>();
            
            // Add text prompt part
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", prompt);
            parts.add(textPart);
            
            // Add ECG image part if available (Multimodal request)
            boolean isMultimodal = ecgImageBase64 != null && !ecgImageBase64.isEmpty();
            if (isMultimodal) {
                Map<String, Object> imagePart = new HashMap<>();
                Map<String, Object> inlineData = new HashMap<>();
                inlineData.put("mimeType", "image/png");
                inlineData.put("data", ecgImageBase64);
                imagePart.put("inlineData", inlineData);
                parts.add(imagePart);
                log.debug("Added ECG image to multimodal request ({} characters)", ecgImageBase64.length());
            }
            
            content.put("parts", parts);
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
            result.put("multimodal", isMultimodal);

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
        prompt.append("You are an expert cardiologist AI assistant. ");
        prompt.append("Your role is to interpret ECG analysis into **clear, factual, and concise** explanations ");
        prompt.append("for patients with ZERO medical knowledge, in Vietnamese language.\n\n");
        
        // ========== MULTIMODAL ECG IMAGE ANALYSIS INSTRUCTIONS ==========
        prompt.append("## ECG Waveform Visual Analysis:\n");
        prompt.append("If an ECG waveform image is provided alongside this text, you MUST:\n");
        prompt.append("1. **Visually inspect** the ECG waveform to identify key patterns (P waves, QRS complexes, T waves, intervals)\n");
        prompt.append("2. **Cross-reference** your visual observations with the numerical features provided below\n");
        prompt.append("3. **Validate** that the AI diagnosis is consistent with what you observe in the waveform\n");
        prompt.append("4. **Describe** specific visual patterns straightforwardly (e.g., 'Hình ảnh sóng điện tim cho thấy nhịp không đều...')\n");
        prompt.append("5. Use the image to provide **objective** medical insights\n\n");
        prompt.append("If no image is provided, proceed with text-only analysis using the numerical data.\n\n");
        
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
        
        prompt.append("**Step 2: Diagnosis Correlation**\n");
        prompt.append("- Cross-reference the detected diagnosis with the feature values\n");
        
        prompt.append("**Step 3: Explanation Strategy**\n");
        prompt.append("- Focus on **physiological mechanisms** (describe WHAT is happening physically in the heart).\n");
        prompt.append("- Translate medical phenomena into **plain, descriptive Vietnamese**.\n\n");
        
        prompt.append("**Step 4: Tone Calibration**\n");
        prompt.append("- Tone: **Professional, objective, and reassuringly calm**.\n");
        prompt.append("- Language: **Literal and precise**. Use specific descriptors rather than analogies or metaphors.\n");
        prompt.append("- Accessibility: Simple Vietnamese suitable for a general audience (Grade 6 reading level).\n\n");
        
        prompt.append("---\n\n");
        
        // ========== FEW-SHOT EXAMPLES ==========
        prompt.append("## Few-Shot Examples:\n\n");
        prompt.append("Learn from these examples. Note: The output is **direct and practical**.\n\n");
        
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
        prompt.append("  \"summary\": \"Kết quả cho thấy nhịp tim của bạn nhanh hơn mức bình thường (Nhịp nhanh xoang), nhưng các chỉ số khác vẫn ổn định.\",\n");
        prompt.append("  \"details\": \"Nhịp tim hiện tại là 105 lần/phút, cao hơn mức chuẩn (60-100 lần/phút). Điều này thường xảy ra khi cơ thể vừa vận động, căng thẳng hoặc dùng chất kích thích (cà phê, thuốc lá). Tuy nhiên, thời gian dẫn truyền điện tim (QRS 0.09s) vẫn trong giới hạn an toàn, cho thấy cấu trúc tim hoạt động bình thường, không có dấu hiệu tắc nghẽn.\",\n");
        prompt.append("  \"recommendations\": \"Bạn nên nghỉ ngơi, hít thở sâu và hạn chế trà, cà phê trong hôm nay. Nếu không có triệu chứng đau ngực hay khó thở, tình trạng này thường không đáng ngại.\",\n");
        prompt.append("  \"risk_level\": \"low\",\n");
        prompt.append("  \"next_steps\": \"Theo dõi nhịp tim khi nghỉ ngơi. Nếu nhịp tim vẫn trên 100 lần/phút khi bạn đã thư giãn hoàn toàn, hãy đi khám chuyên khoa tim mạch.\"\n");
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
        prompt.append("  \"summary\": \"Phát hiện tình trạng Rung nhĩ: Nhịp tim đập nhanh và không đều. Đây là vấn đề cần sự can thiệp của bác sĩ.\",\n");
        prompt.append("  \"details\": \"Kết quả điện tâm đồ cho thấy tín hiệu điện ở tâm nhĩ (buồng trên của tim) bị rối loạn, khiến tim không co bóp nhịp nhàng mà rung lên. Hệ quả là nhịp tim tăng cao lên 132 lần/phút. Việc nhịp tim không đều kéo dài có thể ảnh hưởng đến khả năng bơm máu của tim.\",\n");
        prompt.append("  \"recommendations\": \"Bạn cần gặp bác sĩ sớm để kiểm soát nhịp tim. Tránh làm việc nặng hoặc tập thể dục cường độ cao vào lúc này.\",\n");
        prompt.append("  \"risk_level\": \"medium\",\n");
        prompt.append("  \"next_steps\": \"Đặt lịch khám tim mạch trong tuần này. Nếu thấy đau ngực, khó thở nhiều hoặc chóng mặt, hãy đến bệnh viện ngay lập tức.\"\n");
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
        prompt.append("Analyze THIS specific patient's data based on the instructions above. ");
        prompt.append("Generate a response in Vietnamese following the JSON format.\n\n");
        
        prompt.append("**CRITICAL Requirements:**\n");
        prompt.append("1. Output language: **VIETNAMESE ONLY**.\n");
        prompt.append("2. Style: **Data-driven and Literal**. Explain the direct physiological meaning of the indicators.\n"); 
        prompt.append("3. Specificity: You MUST explicitly mention the patient's specific values (e.g., 'Nhịp tim 105', 'QRS 0.08s').\n");
        prompt.append("4. Clarity: Ensure the explanation is self-contained and logical without relying on figures of speech.\n\n");
        if (retrievedContext != null && !retrievedContext.isBlank()) {
            prompt.append("5. Citation: If you use information from the 'Medical Reference Knowledge' section, you must mention it explicitly (e.g., 'Theo hướng dẫn y khoa...').\n");
        }

        prompt.append("**JSON Response Format:**\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"summary\": \"Tóm tắt ngắn gọn 1-2 câu về tình trạng.\",\n");
        prompt.append("  \"details\": \"Giải thích trực tiếp cơ chế sinh học và ý nghĩa các chỉ số một cách khách quan. Ví dụ: 'Chỉ số A cao phản ánh nhịp tim nhanh...'.\",\n"); 
        prompt.append("  \"recommendations\": \"Lời khuyên cụ thể.\",\n");
        prompt.append("  \"risk_level\": \"low/medium/high\",\n");
        prompt.append("  \"next_steps\": \"Hành động tiếp theo.\"\n");
        prompt.append("}\n");
        prompt.append("```\n\n");
        
        // Keep this constraint because it is technically critical for the parser
        prompt.append("**CRITICAL: Return ONLY the raw JSON object.**");
        
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
