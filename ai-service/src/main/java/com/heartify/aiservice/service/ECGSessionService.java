package com.heartify.aiservice.service;

import com.heartify.aiservice.dto.ECGRecordingDto;
import com.heartify.aiservice.dto.ECGSessionDto;
import com.heartify.aiservice.dto.ExplanationDto;
import com.heartify.aiservice.dto.PredictionDto;
import com.heartify.aiservice.entity.ECGRecording;
import com.heartify.aiservice.entity.ECGSession;
import com.heartify.aiservice.entity.Explanation;
import com.heartify.aiservice.entity.Prediction;
import com.heartify.aiservice.exception.AiServiceException;
import com.heartify.aiservice.exception.ResourceNotFoundException;
import com.heartify.aiservice.repository.ECGRecordingRepository;
import com.heartify.aiservice.repository.ECGSessionRepository;
import com.heartify.aiservice.repository.ExplanationRepository;
import com.heartify.aiservice.repository.PredictionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class ECGSessionService {

    private static final Logger logger = LoggerFactory.getLogger(ECGSessionService.class);

    private final ECGSessionRepository ecgSessionRepository;
    private final ECGRecordingRepository ecgRecordingRepository;
    private final PredictionRepository predictionRepository;
    private final ExplanationRepository explanationRepository;

    @Value("${ai.model.prediction-api-url}")
    private String predictionApiUrl;

    @Value("${ai.model.explanation-api-url}")
    private String explanationApiUrl;

    public ECGSessionService(ECGSessionRepository ecgSessionRepository,
                             ECGRecordingRepository ecgRecordingRepository,
                             PredictionRepository predictionRepository,
                             ExplanationRepository explanationRepository) {
        this.ecgSessionRepository = ecgSessionRepository;
        this.ecgRecordingRepository = ecgRecordingRepository;
        this.predictionRepository = predictionRepository;
        this.explanationRepository = explanationRepository;
    }

    @Transactional
    public ECGSessionDto createECGSession(ECGSessionDto.CreateECGSessionRequest request, Long userId) {
        try {
            logger.info("Starting ECG session creation for user: {}", userId);

            // Step 1: Save ECG Recording
            ECGRecording ecgRecording = ECGRecording.builder()
                    .rawData(request.getRawData())
                    .denoisedData(request.getDenoisedData())
                    .samplingRate(request.getSamplingRate())
                    .build();
            ECGRecording savedRecording = ecgRecordingRepository.save(ecgRecording);
            logger.info("ECG Recording saved with id: {}", savedRecording.getId());

            // Step 2: Call AI Model for Prediction
            // TODO: Implement actual API call to AI prediction model
            // For now, using mock data
            logger.info("Calling AI Prediction Model at: {}", predictionApiUrl);
            Map<String, Object> predictionResponse = callPredictionModel(savedRecording.getDenoisedData());
            
            Prediction prediction = Prediction.builder()
                    .modelVersion((Integer) predictionResponse.get("model_version"))
                    .diagnosis((String) predictionResponse.get("diagnosis"))
                    .probability((Double) predictionResponse.get("probability"))
                    .features((Map<String, Object>) predictionResponse.get("features"))
                    .build();
            Prediction savedPrediction = predictionRepository.save(prediction);
            logger.info("Prediction saved with id: {}", savedPrediction.getId());

            // Step 3: Call LLM for Explanation
            // TODO: Implement actual API call to LLM explanation model
            // For now, using mock data
            logger.info("Calling LLM Explanation Model at: {}", explanationApiUrl);
            Map<String, Object> explanationResponse = callExplanationModel(
                    savedPrediction.getDiagnosis(),
                    savedPrediction.getProbability(),
                    savedPrediction.getFeatures()
            );

            Explanation explanation = Explanation.builder()
                    .llmModelVersion((Integer) explanationResponse.get("llm_model_version"))
                    .prompt(createPrompt(savedPrediction))
                    .explanation((Map<String, Object>) explanationResponse.get("explanation"))
                    .build();
            Explanation savedExplanation = explanationRepository.save(explanation);
            logger.info("Explanation saved with id: {}", savedExplanation.getId());

            // Step 4: Create ECG Session linking all components
            ECGSession session = ECGSession.builder()
                    .userId(userId)
                    .deviceId(request.getDeviceId())
                    .ecgId(savedRecording.getId())
                    .predictionId(savedPrediction.getId())
                    .explanationId(savedExplanation.getId())
                    .build();
            ECGSession savedSession = ecgSessionRepository.save(session);
            logger.info("ECG Session created successfully with id: {}", savedSession.getId());

            return mapToDetailedDto(savedSession, savedRecording, savedPrediction, savedExplanation);

        } catch (Exception e) {
            logger.error("Error creating ECG session: {}", e.getMessage(), e);
            throw new AiServiceException("Failed to create ECG session: " + e.getMessage(), e);
        }
    }

    public Page<ECGSessionDto> getECGSessions(Long userId, Pageable pageable) {
        Page<ECGSession> sessions = ecgSessionRepository.findByUserId(userId, pageable);
        return sessions.map(this::mapToDto);
    }

    public ECGSessionDto getECGSessionById(String id) {
        ECGSession session = ecgSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ECG Session not found with id: " + id));

        // Load related entities
        ECGRecording recording = ecgRecordingRepository.findById(session.getEcgId())
                .orElseThrow(() -> new ResourceNotFoundException("ECG Recording not found"));
        Prediction prediction = predictionRepository.findById(session.getPredictionId())
                .orElseThrow(() -> new ResourceNotFoundException("Prediction not found"));
        Explanation explanation = explanationRepository.findById(session.getExplanationId())
                .orElseThrow(() -> new ResourceNotFoundException("Explanation not found"));

        return mapToDetailedDto(session, recording, prediction, explanation);
    }

    public void deleteECGSession(String id) {
        ECGSession session = ecgSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ECG Session not found with id: " + id));

        // Delete related entities
        ecgRecordingRepository.deleteById(session.getEcgId());
        predictionRepository.deleteById(session.getPredictionId());
        explanationRepository.deleteById(session.getExplanationId());
        
        // Delete session
        ecgSessionRepository.deleteById(id);
        logger.info("ECG Session deleted successfully: {}", id);
    }

    // TODO: Replace this method with actual API call to AI prediction model
    private Map<String, Object> callPredictionModel(Map<String, Object> denoisedData) {
        // Mock response - Replace with actual WebClient call to prediction API
        logger.warn("Using mock prediction data. Implement actual API call to: {}", predictionApiUrl);
        
        Map<String, Object> response = new HashMap<>();
        response.put("model_version", 1);
        response.put("diagnosis", "Normal Sinus Rhythm");
        response.put("probability", 0.95);
        
        Map<String, Object> features = new HashMap<>();
        features.put("heart_rate", 75);
        features.put("qrs_duration", 0.08);
        features.put("pr_interval", 0.16);
        response.put("features", features);
        
        return response;
    }

    // TODO: Replace this method with actual API call to LLM explanation model
    private Map<String, Object> callExplanationModel(String diagnosis, Double probability, Map<String, Object> features) {
        // Mock response - Replace with actual WebClient call to LLM API
        logger.warn("Using mock explanation data. Implement actual API call to: {}", explanationApiUrl);
        
        Map<String, Object> response = new HashMap<>();
        response.put("llm_model_version", 1);
        
        Map<String, Object> explanationContent = new HashMap<>();
        explanationContent.put("summary", "Your ECG shows a normal sinus rhythm with good regularity.");
        explanationContent.put("details", "All cardiac intervals are within normal limits. No signs of arrhythmia detected.");
        explanationContent.put("recommendation", "Continue regular check-ups and maintain a healthy lifestyle.");
        response.put("explanation", explanationContent);
        
        return response;
    }

    private Map<String, Object> createPrompt(Prediction prediction) {
        Map<String, Object> prompt = new HashMap<>();
        prompt.put("diagnosis", prediction.getDiagnosis());
        prompt.put("probability", prediction.getProbability());
        prompt.put("features", prediction.getFeatures());
        return prompt;
    }

    private ECGSessionDto mapToDto(ECGSession session) {
        return ECGSessionDto.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .deviceId(session.getDeviceId())
                .ecgId(session.getEcgId())
                .predictionId(session.getPredictionId())
                .explanationId(session.getExplanationId())
                .createdAt(session.getCreatedAt())
                .build();
    }

    private ECGSessionDto mapToDetailedDto(ECGSession session, ECGRecording recording,
                                           Prediction prediction, Explanation explanation) {
        return ECGSessionDto.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .deviceId(session.getDeviceId())
                .ecgId(session.getEcgId())
                .predictionId(session.getPredictionId())
                .explanationId(session.getExplanationId())
                .createdAt(session.getCreatedAt())
                .ecgRecording(mapRecordingToDto(recording))
                .prediction(mapPredictionToDto(prediction))
                .explanation(mapExplanationToDto(explanation))
                .build();
    }

    private ECGRecordingDto mapRecordingToDto(ECGRecording recording) {
        return ECGRecordingDto.builder()
                .id(recording.getId())
                .rawData(recording.getRawData())
                .denoisedData(recording.getDenoisedData())
                .samplingRate(recording.getSamplingRate())
                .recordedAt(recording.getRecordedAt())
                .build();
    }

    private PredictionDto mapPredictionToDto(Prediction prediction) {
        return PredictionDto.builder()
                .id(prediction.getId())
                .modelVersion(prediction.getModelVersion())
                .diagnosis(prediction.getDiagnosis())
                .probability(prediction.getProbability())
                .features(prediction.getFeatures())
                .createdAt(prediction.getCreatedAt())
                .build();
    }

    private ExplanationDto mapExplanationToDto(Explanation explanation) {
        return ExplanationDto.builder()
                .id(explanation.getId())
                .llmModelVersion(explanation.getLlmModelVersion())
                .prompt(explanation.getPrompt())
                .explanation(explanation.getExplanation())
                .createdAt(explanation.getCreatedAt())
                .build();
    }
}
