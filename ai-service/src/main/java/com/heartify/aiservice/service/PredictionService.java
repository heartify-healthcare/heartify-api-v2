package com.heartify.aiservice.service;

import com.heartify.aiservice.dto.PredictionDto;
import com.heartify.aiservice.entity.Prediction;
import com.heartify.aiservice.exception.ResourceNotFoundException;
import com.heartify.aiservice.repository.PredictionRepository;
import org.springframework.stereotype.Service;

@Service
public class PredictionService {

    private final PredictionRepository predictionRepository;

    public PredictionService(PredictionRepository predictionRepository) {
        this.predictionRepository = predictionRepository;
    }

    public PredictionDto createPrediction(PredictionDto.CreatePredictionRequest request) {
        Prediction prediction = Prediction.builder()
                .modelVersion(request.getModelVersion())
                .diagnosis(request.getDiagnosis())
                .probability(request.getProbability())
                .features(request.getFeatures())
                .build();

        Prediction savedPrediction = predictionRepository.save(prediction);
        return mapToDto(savedPrediction);
    }

    public PredictionDto getPredictionById(String id) {
        Prediction prediction = predictionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prediction not found with id: " + id));
        return mapToDto(prediction);
    }

    public void deletePrediction(String id) {
        if (!predictionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Prediction not found with id: " + id);
        }
        predictionRepository.deleteById(id);
    }

    private PredictionDto mapToDto(Prediction prediction) {
        return PredictionDto.builder()
                .id(prediction.getId())
                .modelVersion(prediction.getModelVersion())
                .diagnosis(prediction.getDiagnosis())
                .probability(prediction.getProbability())
                .features(prediction.getFeatures())
                .createdAt(prediction.getCreatedAt())
                .build();
    }
}
