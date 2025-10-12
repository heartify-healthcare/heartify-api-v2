package com.healthcare.userservice.repository;

import com.healthcare.userservice.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    List<Device> findByUser_UserId(Long userId);
    Optional<Device> findBySerialNumber(String serialNumber);
    boolean existsBySerialNumber(String serialNumber);
}