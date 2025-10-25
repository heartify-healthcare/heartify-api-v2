package com.heartify.aiservice.controller;

import com.heartify.aiservice.dto.ECGSessionDto;
import com.heartify.aiservice.dto.MessageResponse;
import com.heartify.aiservice.service.ECGSessionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/ecg-sessions")
public class ECGSessionController {

    private final ECGSessionService ecgSessionService;

    public ECGSessionController(ECGSessionService ecgSessionService) {
        this.ecgSessionService = ecgSessionService;
    }

    // GET /ecg-sessions - Get all ECG sessions with pagination
    @GetMapping
    public ResponseEntity<Page<ECGSessionDto>> getECGSessions(
            @RequestHeader("X-User-Id") String userIdStr,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        UUID userId = UUID.fromString(userIdStr);
        Sort sort = sortDir.equalsIgnoreCase("asc") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ECGSessionDto> sessions = ecgSessionService.getECGSessions(userId, pageable);
        return ResponseEntity.ok(sessions);
    }

    // GET /ecg-sessions/{id} - Get ECG session by ID with full details
    @GetMapping("/{id}")
    public ResponseEntity<ECGSessionDto> getECGSessionById(@PathVariable String id) {
        ECGSessionDto session = ecgSessionService.getECGSessionById(id);
        return ResponseEntity.ok(session);
    }

    // POST /ecg-sessions - Create new ECG session (Main workflow)
    @PostMapping
    public ResponseEntity<ECGSessionDto> createECGSession(
            @Valid @RequestBody ECGSessionDto.CreateECGSessionRequest request,
            @RequestHeader("X-User-Id") String userIdStr) {

        UUID userId = UUID.fromString(userIdStr);
        ECGSessionDto createdSession = ecgSessionService.createECGSession(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSession);
    }

    // DELETE /ecg-sessions/{id} - Delete ECG session
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteECGSession(@PathVariable String id) {
        ecgSessionService.deleteECGSession(id);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("ECG Session deleted successfully")
                .build());
    }
}
