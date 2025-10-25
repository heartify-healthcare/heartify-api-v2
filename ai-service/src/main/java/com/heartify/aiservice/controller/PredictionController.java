package com.heartify.aiservice.controller;

import com.heartify.aiservice.dto.MessageResponse;
import com.heartify.aiservice.dto.PredictionDto;
import com.heartify.aiservice.service.PredictionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/predictions")
public class PredictionController {

    private final PredictionService predictionService;

    public PredictionController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    // GET /predictions/{id} - Get prediction by ID
    @GetMapping("/{id}")
    public ResponseEntity<PredictionDto> getPredictionById(@PathVariable String id) {
        PredictionDto prediction = predictionService.getPredictionById(id);
        return ResponseEntity.ok(prediction);
    }

    // POST /predictions - Create new prediction
    @PostMapping
    public ResponseEntity<PredictionDto> createPrediction(
            @Valid @RequestBody PredictionDto.CreatePredictionRequest request) {

        PredictionDto createdPrediction = predictionService.createPrediction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPrediction);
    }

    // DELETE /predictions/{id} - Delete prediction
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deletePrediction(@PathVariable String id) {
        predictionService.deletePrediction(id);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Prediction deleted successfully")
                .build());
    }
}
