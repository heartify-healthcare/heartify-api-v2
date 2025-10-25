package com.heartify.aiservice.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Document(collection = "ecg_recordings")
public class ECGRecording {

    @Id
    private String id;

    @Field("raw_data")
    private Map<String, Object> rawData;

    @Field("denoised_data")
    private Map<String, Object> denoisedData;

    @Field("sampling_rate")
    private Integer samplingRate;

    @Field("recorded_at")
    private LocalDateTime recordedAt;

    public ECGRecording() {
        this.id = UUID.randomUUID().toString();
        this.recordedAt = LocalDateTime.now();
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
        private final ECGRecording recording;

        private Builder() {
            recording = new ECGRecording();
        }

        public Builder id(String id) {
            recording.id = id;
            return this;
        }

        public Builder rawData(Map<String, Object> rawData) {
            recording.rawData = rawData;
            return this;
        }

        public Builder denoisedData(Map<String, Object> denoisedData) {
            recording.denoisedData = denoisedData;
            return this;
        }

        public Builder samplingRate(Integer samplingRate) {
            recording.samplingRate = samplingRate;
            return this;
        }

        public Builder recordedAt(LocalDateTime recordedAt) {
            recording.recordedAt = recordedAt;
            return this;
        }

        public ECGRecording build() {
            return recording;
        }
    }
}
