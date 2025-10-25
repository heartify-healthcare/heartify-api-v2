package com.heartify.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.heartify.userservice.entity.OtpVerification;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, UUID> {
    
    Optional<OtpVerification> findByUser_IdAndOtpCodeAndOtpUsedFalse(UUID userId, String otpCode);
    
    Optional<OtpVerification> findFirstByUser_IdOrderByIdDesc(UUID userId);
    
    @Modifying
    @Query("UPDATE OtpVerification o SET o.otpUsed = true WHERE o.user.id = :userId AND o.otpUsed = false")
    void invalidateUserOtps(UUID userId);
    
    @Modifying
    @Query("DELETE FROM OtpVerification o WHERE o.expiredTime < :currentTime")
    void deleteExpiredOtps(Long currentTime);
}