package com.healthcare.userservice.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public class DeviceDto {
    private Long deviceId;
    private Long userId;
    private String deviceName;
    private String deviceType;
    private String serialNumber;
    private String status;
    private LocalDateTime registeredAt;

    public DeviceDto(Long deviceId, Long userId, String deviceName, String deviceType, String serialNumber,
            String status, LocalDateTime registeredAt) {
        this.deviceId = deviceId;
        this.userId = userId;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.serialNumber = serialNumber;
        this.status = status;
        this.registeredAt = registeredAt;
    }

    public static DeviceDto fromEntity(com.healthcare.userservice.entity.Device device) {
        return DeviceDto.builder()
                .deviceId(device.getDeviceId())
                .userId(device.getUser() != null ? device.getUser().getUserId() : null)
                .deviceName(device.getDeviceName())
                .deviceType(device.getDeviceType())
                .serialNumber(device.getSerialNumber())
                .status(device.getStatus() != null ? device.getStatus().name() : null)
                .registeredAt(device.getRegisteredAt())
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long deviceId;
        private Long userId;
        private String deviceName;
        private String deviceType;
        private String serialNumber;
        private String status;
        private LocalDateTime registeredAt;

        public Builder deviceId(Long deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder deviceName(String deviceName) {
            this.deviceName = deviceName;
            return this;
        }

        public Builder deviceType(String deviceType) {
            this.deviceType = deviceType;
            return this;
        }

        public Builder serialNumber(String serialNumber) {
            this.serialNumber = serialNumber;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder registeredAt(LocalDateTime registeredAt) {
            this.registeredAt = registeredAt;
            return this;
        }

        public DeviceDto build() {
            return new DeviceDto(deviceId, userId, deviceName, deviceType, serialNumber, status, registeredAt);
        }
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    public static class CreateDeviceRequest {
        @NotBlank(message = "Device name is required")
        private String deviceName;

        @NotBlank(message = "Device type is required")
        private String deviceType;

        @NotBlank(message = "Serial number is required")
        private String serialNumber;

        @NotBlank(message = "User ID is required")
        private Long userId;

        public String getDeviceName() {
            return deviceName;
        }

        public void setDeviceName(String deviceName) {
            this.deviceName = deviceName;
        }

        public String getDeviceType() {
            return deviceType;
        }

        public void setDeviceType(String deviceType) {
            this.deviceType = deviceType;
        }

        public String getSerialNumber() {
            return serialNumber;
        }

        public void setSerialNumber(String serialNumber) {
            this.serialNumber = serialNumber;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

    }
}