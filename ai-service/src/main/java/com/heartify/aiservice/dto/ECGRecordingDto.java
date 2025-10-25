package com.heartify.aiservice.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;

public class ECGRecordingDto {

    private String id;
    private Map<String, Object> rawData;
    private Map<String, Object> denoisedData;
    private Integer samplingRate;
    private LocalDateTime recordedAt;

    public ECGRecordingDto() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, Object> getRawData() {
        return rawData;
    }

    public void setRawData(Map<String, Object> rawData) {
        this.rawData = rawData;
    }

    public Map<String, Object> getDenoisedData() {
        return denoisedData;
    }

    public void setDenoisedData(Map<String, Object> denoisedData) {
        this.denoisedData = denoisedData;
    }

    public Integer getSamplingRate() {
        return samplingRate;
    }

    public void setSamplingRate(Integer samplingRate) {
        this.samplingRate = samplingRate;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ECGRecordingDto dto;

        private Builder() {
            dto = new ECGRecordingDto();
        }

        public Builder id(String id) {
            dto.id = id;
            return this;
        }

        public Builder rawData(Map<String, Object> rawData) {
            dto.rawData = rawData;
            return this;
        }

        public Builder denoisedData(Map<String, Object> denoisedData) {
            dto.denoisedData = denoisedData;
            return this;
        }

        public Builder samplingRate(Integer samplingRate) {
            dto.samplingRate = samplingRate;
            return this;
        }

        public Builder recordedAt(LocalDateTime recordedAt) {
            dto.recordedAt = recordedAt;
            return this;
        }

        public ECGRecordingDto build() {
            return dto;
        }
    }

    public static class CreateECGRecordingRequest {
        @NotNull(message = "Raw data is required")
        private Map<String, Object> rawData;

        @NotNull(message = "Denoised data is required")
        private Map<String, Object> denoisedData;

        @NotNull(message = "Sampling rate is required")
        private Integer samplingRate;

        public CreateECGRecordingRequest() {
        }

        public Map<String, Object> getRawData() {
            return rawData;
        }

        public void setRawData(Map<String, Object> rawData) {
            this.rawData = rawData;
        }

        public Map<String, Object> getDenoisedData() {
            return denoisedData;
        }

        public void setDenoisedData(Map<String, Object> denoisedData) {
            this.denoisedData = denoisedData;
        }

        public Integer getSamplingRate() {
            return samplingRate;
        }

        public void setSamplingRate(Integer samplingRate) {
            this.samplingRate = samplingRate;
        }
    }
}
