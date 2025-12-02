package com.heartify.aiservice.controller;

import com.heartify.aiservice.dto.ExplanationDto;
import com.heartify.aiservice.dto.MessageResponse;
import com.heartify.aiservice.service.ExplanationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/explanations")
public class ExplanationController {

    private final ExplanationService explanationService;

    public ExplanationController(ExplanationService explanationService) {
        this.explanationService = explanationService;
    }

    // GET /explanations/{id} - Get explanation by ID
    @GetMapping("/{id}")
    public ResponseEntity<ExplanationDto> getExplanationById(@PathVariable String id) {
        ExplanationDto explanation = explanationService.getExplanationById(id);
        return ResponseEntity.ok(explanation);
    }

    // DELETE /explanations/{id} - Delete explanation
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteExplanation(@PathVariable String id) {
        explanationService.deleteExplanation(id);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Explanation deleted successfully")
                .build());
    }
}
