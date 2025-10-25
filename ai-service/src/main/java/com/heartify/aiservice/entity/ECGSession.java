package com.heartify.aiservice.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "ecg_sessions")
public class ECGSession {

    @Id
    private String id;

    @Field("user_id")
    private UUID userId;

    @Field("device_id")
    private String deviceId;

    @Field("ecg_id")
    private String ecgId;

    @Field("prediction_id")
    private String predictionId;

    @Field("explanation_id")
    private String explanationId;

    @Field("created_at")
    private LocalDateTime createdAt;

    public ECGSession() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ECGSession session;

        private Builder() {
            session = new ECGSession();
        }

        public Builder id(String id) {
            session.id = id;
            return this;
        }

        public Builder userId(UUID userId) {
            session.userId = userId;
            return this;
        }

        public Builder deviceId(String deviceId) {
            session.deviceId = deviceId;
            return this;
        }

        public Builder ecgId(String ecgId) {
            session.ecgId = ecgId;
            return this;
        }

        public Builder predictionId(String predictionId) {
            session.predictionId = predictionId;
            return this;
        }

        public Builder explanationId(String explanationId) {
            session.explanationId = explanationId;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            session.createdAt = createdAt;
            return this;
        }

        public ECGSession build() {
            return session;
        }
    }
}
