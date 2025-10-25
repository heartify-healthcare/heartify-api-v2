package com.heartify.aiservice.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;

public class PredictionDto {

    private String id;
    private Integer modelVersion;
    private String diagnosis;
    private Double probability;
    private Map<String, Object> features;
    private LocalDateTime createdAt;

    public PredictionDto() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(Integer modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public Double getProbability() {
        return probability;
    }

    public void setProbability(Double probability) {
        this.probability = probability;
    }

    public Map<String, Object> getFeatures() {
        return features;
    }

    public void setFeatures(Map<String, Object> features) {
        this.features = features;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final PredictionDto dto;

        private Builder() {
            dto = new PredictionDto();
        }

        public Builder id(String id) {
            dto.id = id;
            return this;
        }

        public Builder modelVersion(Integer modelVersion) {
            dto.modelVersion = modelVersion;
            return this;
        }

        public Builder diagnosis(String diagnosis) {
            dto.diagnosis = diagnosis;
            return this;
        }

        public Builder probability(Double probability) {
            dto.probability = probability;
            return this;
        }

        public Builder features(Map<String, Object> features) {
            dto.features = features;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            dto.createdAt = createdAt;
            return this;
        }

        public PredictionDto build() {
            return dto;
        }
    }

    public static class CreatePredictionRequest {
        @NotNull(message = "Model version is required")
        private Integer modelVersion;

        @NotNull(message = "Diagnosis is required")
        private String diagnosis;

        @NotNull(message = "Probability is required")
        private Double probability;

        @NotNull(message = "Features are required")
        private Map<String, Object> features;

        public CreatePredictionRequest() {
        }

        public Integer getModelVersion() {
            return modelVersion;
        }

        public void setModelVersion(Integer modelVersion) {
            this.modelVersion = modelVersion;
        }

        public String getDiagnosis() {
            return diagnosis;
        }

        public void setDiagnosis(String diagnosis) {
            this.diagnosis = diagnosis;
        }

        public Double getProbability() {
            return probability;
        }

        public void setProbability(Double probability) {
            this.probability = probability;
        }

        public Map<String, Object> getFeatures() {
            return features;
        }

        public void setFeatures(Map<String, Object> features) {
            this.features = features;
        }
    }
}
