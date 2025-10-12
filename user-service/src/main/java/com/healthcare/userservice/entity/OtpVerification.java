package com.healthcare.userservice.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_verifications")
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private OtpVerification() {
        // Private constructor for Builder pattern
    }

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String otpCode;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Boolean verified = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
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

        private Builder() {
            otpVerification = new OtpVerification();
        }

        public Builder email(String email) {
            otpVerification.email = email;
            return this;
        }

        public Builder otpCode(String otpCode) {
            otpVerification.otpCode = otpCode;
            return this;
        }

        public Builder expiresAt(LocalDateTime expiresAt) {
            otpVerification.expiresAt = expiresAt;
            return this;
        }

        public Builder verified(Boolean verified) {
            otpVerification.verified = verified;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            otpVerification.createdAt = createdAt;
            return this;
        }

        public OtpVerification build() {
            return otpVerification;
        }
    }
}