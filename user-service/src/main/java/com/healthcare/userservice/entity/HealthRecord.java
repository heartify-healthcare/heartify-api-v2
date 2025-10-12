package com.healthcare.userservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "health_records")
public class HealthRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long recordId;
    
    private HealthRecord() {
        // Private constructor for Builder pattern
    }
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "heart_rate")
    private Integer heartRate;
    
    @Column(name = "blood_pressure", length = 20)
    private String bloodPressure;
    
    @Column(name = "temperature")
    private Double temperature;
    
    @Column(name = "spo2")
    private Integer spo2;
    
    @CreationTimestamp
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private LocalDateTime recordedAt;
    
    @Column(columnDefinition = "TEXT")
    private String note;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(Integer heartRate) {
        this.heartRate = heartRate;
    }

    public String getBloodPressure() {
        return bloodPressure;
    }

    public void setBloodPressure(String bloodPressure) {
        this.bloodPressure = bloodPressure;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getSpo2() {
        return spo2;
    }

    public void setSpo2(Integer spo2) {
        this.spo2 = spo2;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final HealthRecord healthRecord;

        private Builder() {
            healthRecord = new HealthRecord();
        }

        public Builder user(User user) {
            healthRecord.user = user;
            return this;
        }

        public Builder heartRate(Integer heartRate) {
            healthRecord.heartRate = heartRate;
            return this;
        }

        public Builder bloodPressure(String bloodPressure) {
            healthRecord.bloodPressure = bloodPressure;
            return this;
        }

        public Builder temperature(Double temperature) {
            healthRecord.temperature = temperature;
            return this;
        }

        public Builder spo2(Integer spo2) {
            healthRecord.spo2 = spo2;
            return this;
        }

        public Builder note(String note) {
            healthRecord.note = note;
            return this;
        }

        public HealthRecord build() {
            return healthRecord;
        }
    }
}