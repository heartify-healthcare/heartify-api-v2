package com.healthcare.userservice.repository;

import com.healthcare.userservice.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    Optional<OtpVerification> findByEmailAndVerifiedFalse(String email);
    void deleteByEmail(String email);
}