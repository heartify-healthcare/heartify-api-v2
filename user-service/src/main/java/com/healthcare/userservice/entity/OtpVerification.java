package com.healthcare.userservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "otps")
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private OtpVerification() {
        // Private constructor for Builder pattern
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "otp_code", nullable = false)
    private String otpCode;

    @Column(name = "expired_time", nullable = false)
    private Long expiredTime; // UNIX timestamp in seconds

    @Column(name = "otp_used", nullable = false)
    private Boolean otpUsed = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    public Long getExpiredTime() {
        return expiredTime;
    }

    public void setExpiredTime(Long expiredTime) {
        this.expiredTime = expiredTime;
    }

    public Boolean getOtpUsed() {
        return otpUsed;
    }

    public void setOtpUsed(Boolean otpUsed) {
        this.otpUsed = otpUsed;
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
        private final OtpVerification otpVerification;

        public Builder() {
            otpVerification = new OtpVerification();
        }

        public Builder user(User user) {
            otpVerification.setUser(user);
            return this;
        }

        public Builder otpCode(String otpCode) {
            otpVerification.setOtpCode(otpCode);
            return this;
        }

        public Builder expiredTime(Long expiredTime) {
            otpVerification.setExpiredTime(expiredTime);
            return this;
        }

        public Builder otpUsed(Boolean otpUsed) {
            otpVerification.setOtpUsed(otpUsed);
            return this;
        }

        public OtpVerification build() {
            return otpVerification;
        }
    }
}