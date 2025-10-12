package com.healthcare.userservice.dto;

import java.time.LocalDateTime;

import com.healthcare.userservice.entity.User;

public class UserDto {
    private Long userId;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String role;
    private Boolean isVerified;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private UserDto() {
        // Private constructor for Builder pattern
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final UserDto userDto;

        private Builder() {
            userDto = new UserDto();
        }

        public Builder userId(Long userId) {
            userDto.userId = userId;
            return this;
        }

        public Builder email(String email) {
            userDto.email = email;
            return this;
        }

        public Builder fullName(String fullName) {
            userDto.fullName = fullName;
            return this;
        }

        public Builder phoneNumber(String phoneNumber) {
            userDto.phoneNumber = phoneNumber;
            return this;
        }

        public Builder role(String role) {
            userDto.role = role;
            return this;
        }

        public Builder isVerified(Boolean isVerified) {
            userDto.isVerified = isVerified;
            return this;
        }

        public Builder status(String status) {
            userDto.status = status;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            userDto.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            userDto.updatedAt = updatedAt;
            return this;
        }

        public UserDto build() {
            return userDto;
        }
    }

    public static UserDto fromEntity(User user) {
        return UserDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().name())
                .isVerified(user.getIsVerified())
                .status(user.getStatus().name())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(Boolean isVerified) {
        this.isVerified = isVerified;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

    public static class UpdateUserRequest {
        private String fullName;
        private String phoneNumber;

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

    }
}