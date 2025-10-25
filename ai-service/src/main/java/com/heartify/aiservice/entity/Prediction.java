package com.heartify.aiservice.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Document(collection = "predictions")
public class Prediction {

    @Id
    private String id;

    @Field("model_version")
    private Integer modelVersion;

    @Field("diagnosis")
    private String diagnosis;

    @Field("probability")
    private Double probability;

    @Field("features")
    private Map<String, Object> features;

    @Field("created_at")
    private LocalDateTime createdAt;

    public Prediction() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
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
        private final Prediction prediction;

        private Builder() {
            prediction = new Prediction();
        }

        public Builder id(String id) {
            prediction.id = id;
            return this;
        }

        public Builder modelVersion(Integer modelVersion) {
            prediction.modelVersion = modelVersion;
            return this;
        }

        public Builder diagnosis(String diagnosis) {
            prediction.diagnosis = diagnosis;
            return this;
        }

        public Builder probability(Double probability) {
            prediction.probability = probability;
            return this;
        }

        public Builder features(Map<String, Object> features) {
            prediction.features = features;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            prediction.createdAt = createdAt;
            return this;
        }

        public Prediction build() {
            return prediction;
        }
    }
}
