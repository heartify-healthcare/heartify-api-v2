package com.healthcare.userservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;
    
    private User() {
        // Private constructor for Builder pattern
    }
    
    @Column(unique = true, nullable = false, length = 100)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;
    
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;
    
    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Device> devices;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HealthRecord> healthRecords;
    
    public enum UserRole {
        USER, CLINICIAN, ADMIN
    }
    
    public enum UserStatus {
        ACTIVE, INACTIVE
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Boolean getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(Boolean isVerified) {
        this.isVerified = isVerified;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<Device> getDevices() {
        return devices;
    }

    public void setDevices(List<Device> devices) {
        this.devices = devices;
    }

    public List<HealthRecord> getHealthRecords() {
        return healthRecords;
    }

    public void setHealthRecords(List<HealthRecord> healthRecords) {
        this.healthRecords = healthRecords;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final User user;

        private Builder() {
            user = new User();
        }

        public Builder email(String email) {
            user.email = email;
            return this;
        }

        public Builder password(String password) {
            user.password = password;
            return this;
        }

        public Builder fullName(String fullName) {
            user.fullName = fullName;
            return this;
        }

        public Builder phoneNumber(String phoneNumber) {
            user.phoneNumber = phoneNumber;
            return this;
        }

        public Builder role(UserRole role) {
            user.role = role;
            return this;
        }

        public Builder isVerified(Boolean isVerified) {
            user.isVerified = isVerified;
            return this;
        }

        public Builder status(UserStatus status) {
            user.status = status;
            return this;
        }

        public Builder devices(List<Device> devices) {
            user.devices = devices;
            return this;
        }

        public Builder healthRecords(List<HealthRecord> healthRecords) {
            user.healthRecords = healthRecords;
            return this;
        }

        public User build() {
            return user;
        }
    }
}