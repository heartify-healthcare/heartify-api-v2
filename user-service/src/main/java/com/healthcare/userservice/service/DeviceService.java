package com.healthcare.userservice.service;

import com.healthcare.userservice.dto.DeviceDto;
import com.healthcare.userservice.entity.Device;
import com.healthcare.userservice.entity.User;
import com.healthcare.userservice.repository.DeviceRepository;
import com.healthcare.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;

    public DeviceService(DeviceRepository deviceRepository, UserRepository userRepository) {
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public DeviceDto registerDevice(DeviceDto.CreateDeviceRequest request) {
        if (deviceRepository.existsBySerialNumber(request.getSerialNumber())) {
            throw new RuntimeException("Device with this serial number already exists");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Device device = Device.builder()
                .user(user)
                .deviceName(request.getDeviceName())
                .deviceType(request.getDeviceType())
                .serialNumber(request.getSerialNumber())
                .status(Device.DeviceStatus.ACTIVE)
                .build();

        device = deviceRepository.save(device);
        return DeviceDto.fromEntity(device);
    }

    public List<DeviceDto> getUserDevices(Long userId) {
        List<Device> devices = deviceRepository.findByUser_UserId(userId);
        return devices.stream()
                .map(DeviceDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteDevice(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));
        device.setStatus(Device.DeviceStatus.INACTIVE);
        deviceRepository.save(device);
    }
}