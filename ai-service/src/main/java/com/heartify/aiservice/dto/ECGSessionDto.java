package com.heartify.aiservice.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class ECGSessionDto {

    private String id;
    private UUID userId;
    private String deviceId;
    private String ecgId;
    private String predictionId;
    private String explanationId;
    private LocalDateTime createdAt;

    // Full session details
    private ECGRecordingDto ecgRecording;
    private PredictionDto prediction;
    private ExplanationDto explanation;

    public ECGSessionDto() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getEcgId() {
        return ecgId;
    }

    public void setEcgId(String ecgId) {
        this.ecgId = ecgId;
    }

    public String getPredictionId() {
        return predictionId;
    }

    public void setPredictionId(String predictionId) {
        this.predictionId = predictionId;
    }

    public String getExplanationId() {
        return explanationId;
    }

    public void setExplanationId(String explanationId) {
        this.explanationId = explanationId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ECGRecordingDto getEcgRecording() {
        return ecgRecording;
    }

    public void setEcgRecording(ECGRecordingDto ecgRecording) {
        this.ecgRecording = ecgRecording;
    }

    public PredictionDto getPrediction() {
        return prediction;
    }

    public void setPrediction(PredictionDto prediction) {
        this.prediction = prediction;
    }

    public ExplanationDto getExplanation() {
        return explanation;
    }

    public void setExplanation(ExplanationDto explanation) {
        this.explanation = explanation;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ECGSessionDto dto;

        private Builder() {
            dto = new ECGSessionDto();
        }

        public Builder id(String id) {
            dto.id = id;
            return this;
        }

        public Builder userId(UUID userId) {
            dto.userId = userId;
            return this;
        }

        public Builder deviceId(String deviceId) {
            dto.deviceId = deviceId;
            return this;
        }

        public Builder ecgId(String ecgId) {
            dto.ecgId = ecgId;
            return this;
        }

        public Builder predictionId(String predictionId) {
            dto.predictionId = predictionId;
            return this;
        }

        public Builder explanationId(String explanationId) {
            dto.explanationId = explanationId;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            dto.createdAt = createdAt;
            return this;
        }

        public Builder ecgRecording(ECGRecordingDto ecgRecording) {
            dto.ecgRecording = ecgRecording;
            return this;
        }

        public Builder prediction(PredictionDto prediction) {
            dto.prediction = prediction;
            return this;
        }

        public Builder explanation(ExplanationDto explanation) {
            dto.explanation = explanation;
            return this;
        }

        public ECGSessionDto build() {
            return dto;
        }
    }

    public static class CreateECGSessionRequest {
        @NotNull(message = "Device ID is required")
        private String deviceId;

        @NotNull(message = "Raw data is required")
        private Map<String, Object> rawData;

        @NotNull(message = "Sampling rate is required")
        private Integer samplingRate;

        public CreateECGSessionRequest() {
        }

        public String getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }

        public Map<String, Object> getRawData() {
            return rawData;
        }

        public void setRawData(Map<String, Object> rawData) {
            this.rawData = rawData;
        }

        public Integer getSamplingRate() {
            return samplingRate;
        }

        public void setSamplingRate(Integer samplingRate) {
            this.samplingRate = samplingRate;
        }
    }
}
