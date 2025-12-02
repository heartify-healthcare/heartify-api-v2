package com.heartify.aiservice.controller;

import com.heartify.aiservice.dto.ECGRecordingDto;
import com.heartify.aiservice.dto.MessageResponse;
import com.heartify.aiservice.service.ECGRecordingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ecg-recordings")
public class ECGRecordingController {

    private final ECGRecordingService ecgRecordingService;

    public ECGRecordingController(ECGRecordingService ecgRecordingService) {
        this.ecgRecordingService = ecgRecordingService;
    }

    // GET /ecg-recordings/{id} - Get ECG recording by ID
    @GetMapping("/{id}")
    public ResponseEntity<ECGRecordingDto> getECGRecordingById(@PathVariable String id) {
        ECGRecordingDto recording = ecgRecordingService.getECGRecordingById(id);
        return ResponseEntity.ok(recording);
    }

    // DELETE /ecg-recordings/{id} - Delete ECG recording
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteECGRecording(@PathVariable String id) {
        ecgRecordingService.deleteECGRecording(id);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("ECG Recording deleted successfully")
                .build());
    }
}
