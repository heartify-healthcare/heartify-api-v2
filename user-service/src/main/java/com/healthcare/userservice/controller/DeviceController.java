package com.healthcare.userservice.controller;

import com.healthcare.userservice.dto.DeviceDto;
import com.healthcare.userservice.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping
    public ResponseEntity<DeviceDto> registerDevice(
            @Valid @RequestBody DeviceDto.CreateDeviceRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        // Only ADMIN can register devices (or allow unauthenticated as per your spec)
        // Based on your API spec, this endpoint has FALSE authentication, so we skip role check
        return ResponseEntity.status(HttpStatus.CREATED).body(deviceService.registerDevice(request));
    }

    @GetMapping
    public ResponseEntity<List<DeviceDto>> getUserDevices(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(deviceService.getUserDevices(userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") String role) {
        // Only ADMIN can delete devices
        if (!role.equals("ADMIN")) {
            return ResponseEntity.status(403).build();
        }
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }
}