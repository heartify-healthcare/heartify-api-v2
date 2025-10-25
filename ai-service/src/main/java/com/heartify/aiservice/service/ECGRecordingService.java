package com.heartify.aiservice.service;

import com.heartify.aiservice.dto.ECGRecordingDto;
import com.heartify.aiservice.entity.ECGRecording;
import com.heartify.aiservice.exception.ResourceNotFoundException;
import com.heartify.aiservice.repository.ECGRecordingRepository;
import org.springframework.stereotype.Service;

@Service
public class ECGRecordingService {

    private final ECGRecordingRepository ecgRecordingRepository;

    public ECGRecordingService(ECGRecordingRepository ecgRecordingRepository) {
        this.ecgRecordingRepository = ecgRecordingRepository;
    }

    public ECGRecordingDto createECGRecording(ECGRecordingDto.CreateECGRecordingRequest request) {
        ECGRecording recording = ECGRecording.builder()
                .rawData(request.getRawData())
                .denoisedData(request.getDenoisedData())
                .samplingRate(request.getSamplingRate())
                .build();

        ECGRecording savedRecording = ecgRecordingRepository.save(recording);
        return mapToDto(savedRecording);
    }

    public ECGRecordingDto getECGRecordingById(String id) {
        ECGRecording recording = ecgRecordingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ECG Recording not found with id: " + id));
        return mapToDto(recording);
    }

    public void deleteECGRecording(String id) {
        if (!ecgRecordingRepository.existsById(id)) {
            throw new ResourceNotFoundException("ECG Recording not found with id: " + id);
        }
        ecgRecordingRepository.deleteById(id);
    }

    private ECGRecordingDto mapToDto(ECGRecording recording) {
        return ECGRecordingDto.builder()
                .id(recording.getId())
                .rawData(recording.getRawData())
                .denoisedData(recording.getDenoisedData())
                .samplingRate(recording.getSamplingRate())
                .recordedAt(recording.getRecordedAt())
                .build();
    }
}
