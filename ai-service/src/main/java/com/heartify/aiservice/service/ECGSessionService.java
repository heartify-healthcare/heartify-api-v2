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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ECGSessionService {

    private final ECGSessionRepository ecgSessionRepository;
    private final ECGRecordingRepository ecgRecordingRepository;
    private final PredictionRepository predictionRepository;
    private final ExplanationRepository explanationRepository;
    private final DenoisingClient denoisingClient;
    private final DLModelClient dlModelClient;
    private final LLMClient llmClient;

    public ECGSessionService(ECGSessionRepository ecgSessionRepository,
                             ECGRecordingRepository ecgRecordingRepository,
                             PredictionRepository predictionRepository,
                             ExplanationRepository explanationRepository,
                             DenoisingClient denoisingClient,
                             DLModelClient dlModelClient,
                             LLMClient llmClient) {
        this.ecgSessionRepository = ecgSessionRepository;
        this.ecgRecordingRepository = ecgRecordingRepository;
        this.predictionRepository = predictionRepository;
        this.explanationRepository = explanationRepository;
        this.denoisingClient = denoisingClient;
        this.dlModelClient = dlModelClient;
        this.llmClient = llmClient;
    }

    @Transactional(rollbackFor = Exception.class)
    public ECGSessionDto createECGSession(ECGSessionDto.CreateECGSessionRequest request, UUID userId) {
        ECGRecording savedRecording = null;
        Prediction savedPrediction = null;
        Explanation savedExplanation = null;
        
        try {
            // Step 1: Extract raw ECG signal from request
            List<Double> rawEcgSignal = extractECGSignal(request.getRawData());
            
            // Step 2: Call Denoising Model to clean ECG signal
            Map<String, Object> denoisingResponse = denoisingClient.denoise(rawEcgSignal);
            @SuppressWarnings("unchecked")
            List<Double> denoisedSignal = (List<Double>) denoisingResponse.get("denoised_signal");
            
            // Step 3: Call Deep Learning Model for Prediction using denoised signal
            Map<String, Object> predictionResponse = dlModelClient.predict(denoisedSignal);
            
            // Step 4: Call LLM API for Explanation
            String diagnosis = (String) predictionResponse.get("diagnosis");
            Double probability = (Double) predictionResponse.get("probability");
            @SuppressWarnings("unchecked")
            Map<String, Object> features = (Map<String, Object>) predictionResponse.get("features");
            
            // Get ECG image for multimodal AI (transient, not stored in DB)
            String ecgImageBase64 = (String) predictionResponse.get("ecg_image_base64");
            
            Map<String, Object> explanationResponse = llmClient.generateExplanation(
                    diagnosis,
                    probability,
                    features,
                    ecgImageBase64
            );

            // Step 5: All API calls succeeded - Now save to database
            
            // Build denoisedData map for storage (same structure as rawData)
            Map<String, Object> denoisedData = new HashMap<>();
            denoisedData.put("signal", denoisedSignal);
            denoisedData.put("lead", request.getRawData().get("lead"));
            denoisedData.put("duration", request.getRawData().get("duration"));
            
            // Save ECG Recording
            ECGRecording ecgRecording = ECGRecording.builder()
                    .rawData(request.getRawData())
                    .denoisedData(denoisedData)
                    .samplingRate(request.getSamplingRate())
                    .build();
            savedRecording = ecgRecordingRepository.save(ecgRecording);
            
            // Save Prediction
            Prediction prediction = Prediction.builder()
                    .modelVersion((Integer) predictionResponse.get("model_version"))
                    .diagnosis(diagnosis)
                    .probability(probability)
                    .features(features)
                    .build();
            savedPrediction = predictionRepository.save(prediction);

            // Save Explanation
            @SuppressWarnings("unchecked")
            Explanation explanation = Explanation.builder()
                    .llmModelVersion((Integer) explanationResponse.get("llm_model_version"))
                    .prompt(createPrompt(savedPrediction))
                    .explanation((Map<String, Object>) explanationResponse.get("explanation"))
                    .build();
            savedExplanation = explanationRepository.save(explanation);

            // Save ECG Session
            ECGSession session = ECGSession.builder()
                    .userId(userId)
                    .deviceId(request.getDeviceId())
                    .ecgId(savedRecording.getId())
                    .predictionId(savedPrediction.getId())
                    .explanationId(savedExplanation.getId())
                    .build();
            ECGSession savedSession = ecgSessionRepository.save(session);

            return mapToDetailedDto(savedSession, savedRecording, savedPrediction, savedExplanation);

        } catch (Exception e) {
            
            // Cleanup: Delete any saved entities if transaction fails
            try {
                if (savedExplanation != null && savedExplanation.getId() != null) {
                    explanationRepository.deleteById(savedExplanation.getId());
                }
                if (savedPrediction != null && savedPrediction.getId() != null) {
                    predictionRepository.deleteById(savedPrediction.getId());
                }
                if (savedRecording != null && savedRecording.getId() != null) {
                    ecgRecordingRepository.deleteById(savedRecording.getId());
                }
            } catch (Exception cleanupException) {
            }
            
            throw new AiServiceException("Failed to create ECG session: " + e.getMessage(), e);
        }
    }

    public Page<ECGSessionDto> getECGSessions(UUID userId, Pageable pageable) {
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
    }

    /**
     * Extract ECG signal from raw data
     * Assumes rawData contains an "ecg_signal" or "signal" field with array of values
     */
    private List<Double> extractECGSignal(Map<String, Object> rawData) {
        // Try to get ecg_signal field
        Object signal = rawData.get("ecg_signal");
        if (signal == null) {
            signal = rawData.get("signal");
        }
        if (signal == null) {
            signal = rawData.get("data");
        }
        
        if (signal instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> rawList = (List<Object>) signal;
            return rawList.stream()
                    .map(obj -> {
                        if (obj instanceof Number) {
                            return ((Number) obj).doubleValue();
                        }
                        return Double.parseDouble(obj.toString());
                    })
                    .toList();
        }
        
        throw new AiServiceException("Invalid ECG signal format in rawData");
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
