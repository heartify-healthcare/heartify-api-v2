package com.heartify.aiservice.service;

import com.heartify.aiservice.dto.ExplanationDto;
import com.heartify.aiservice.entity.Explanation;
import com.heartify.aiservice.exception.ResourceNotFoundException;
import com.heartify.aiservice.repository.ExplanationRepository;
import org.springframework.stereotype.Service;

@Service
public class ExplanationService {

    private final ExplanationRepository explanationRepository;

    public ExplanationService(ExplanationRepository explanationRepository) {
        this.explanationRepository = explanationRepository;
    }

    public ExplanationDto createExplanation(ExplanationDto.CreateExplanationRequest request) {
        Explanation explanation = Explanation.builder()
                .llmModelVersion(request.getLlmModelVersion())
                .prompt(request.getPrompt())
                .explanation(request.getExplanation())
                .build();

        Explanation savedExplanation = explanationRepository.save(explanation);
        return mapToDto(savedExplanation);
    }

    public ExplanationDto getExplanationById(String id) {
        Explanation explanation = explanationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Explanation not found with id: " + id));
        return mapToDto(explanation);
    }

    public void deleteExplanation(String id) {
        if (!explanationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Explanation not found with id: " + id);
        }
        explanationRepository.deleteById(id);
    }

    private ExplanationDto mapToDto(Explanation explanation) {
        return ExplanationDto.builder()
                .id(explanation.getId())
                .llmModelVersion(explanation.getLlmModelVersion())
                .prompt(explanation.getPrompt())
                .explanation(explanation.getExplanation())
                .createdAt(explanation.getCreatedAt())
                .build();
    }
}
