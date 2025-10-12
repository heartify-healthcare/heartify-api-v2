package com.healthcare.userservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "devices")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_id")
    private Long deviceId;
    
    private Device() {
        // Private constructor for Builder pattern
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_name", nullable = false, length = 100)
    private String deviceName;

    @Column(name = "device_type", nullable = false, length = 50)
    private String deviceType;

    @Column(name = "serial_number", unique = true, nullable = false, length = 100)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeviceStatus status = DeviceStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    public enum DeviceStatus {
        ACTIVE, INACTIVE
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public DeviceStatus getStatus() {
        return status;
    }

    public void setStatus(DeviceStatus status) {
        this.status = status;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Device device;

        private Builder() {
            device = new Device();
        }

        public Builder user(User user) {
            device.user = user;
            return this;
        }

        public Builder deviceName(String deviceName) {
            device.deviceName = deviceName;
            return this;
        }

        public Builder deviceType(String deviceType) {
            device.deviceType = deviceType;
            return this;
        }

        public Builder serialNumber(String serialNumber) {
            device.serialNumber = serialNumber;
            return this;
        }

        public Builder status(DeviceStatus status) {
            device.status = status;
            return this;
        }

        public Device build() {
            return device;
        }
    }
}