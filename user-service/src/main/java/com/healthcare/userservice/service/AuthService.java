package com.healthcare.userservice.service;

import com.healthcare.userservice.dto.AuthDto;
import com.healthcare.userservice.dto.UserDto;
import com.healthcare.userservice.entity.OtpVerification;
import com.healthcare.userservice.entity.RefreshToken;
import com.healthcare.userservice.entity.User;
import com.healthcare.userservice.repository.OtpVerificationRepository;
import com.healthcare.userservice.repository.RefreshTokenRepository;
import com.healthcare.userservice.repository.UserRepository;
import com.healthcare.userservice.util.JwtUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OtpVerificationRepository otpRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository,
            OtpVerificationRepository otpRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
    }

    @Value("${otp.expiration}")
    private Long otpExpiration;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Transactional
    public AuthDto.MessageResponse register(AuthDto.RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Create user (not verified yet)
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .role(User.UserRole.USER)
                .isVerified(false)
                .status(User.UserStatus.ACTIVE)
                .build();

        userRepository.save(user);

        // Generate and save OTP
        String otpCode = generateOtpCode();
        OtpVerification otp = OtpVerification.builder()
                .email(request.getEmail())
                .otpCode(otpCode)
                .expiresAt(LocalDateTime.now().plusSeconds(otpExpiration / 1000))
                .verified(false)
                .createdAt(LocalDateTime.now())
                .build();

        otpRepository.save(otp);

        // Send OTP via email
        emailService.sendOtpEmail(request.getEmail(), otpCode);

        return AuthDto.MessageResponse.builder()
                .message("Registration successful. Please check your email for OTP verification.")
                .build();
    }

    @Transactional
    public AuthDto.MessageResponse verifyOtp(AuthDto.VerifyOtpRequest request) {
        OtpVerification otp = otpRepository.findByEmailAndVerifiedFalse(request.getEmail())
                .orElseThrow(() -> new RuntimeException("OTP not found or already verified"));

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired");
        }

        if (!otp.getOtpCode().equals(request.getOtpCode())) {
            throw new RuntimeException("Invalid OTP code");
        }

        // Mark OTP as verified
        otp.setVerified(true);
        otpRepository.save(otp);

        // Update user verification status
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsVerified(true);
        userRepository.save(user);

        return AuthDto.MessageResponse.builder()
                .message("Email verified successfully. You can now login.")
                .build();
    }

    @Transactional
    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!user.getIsVerified()) {
            throw new RuntimeException("Email not verified. Please verify your email first.");
        }

        if (user.getStatus() == User.UserStatus.INACTIVE) {
            throw new RuntimeException("Account is inactive");
        }

        String accessToken = jwtUtil.generateToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        // Save refresh token
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .token(refreshToken)
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(604800)) // 7 days
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        return AuthDto.AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration)
                .user(UserDto.fromEntity(user))
                .build();
    }

    @Transactional
    public AuthDto.MessageResponse forgotPassword(AuthDto.ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate OTP for password reset
        String otpCode = generateOtpCode();

        // Delete old OTP if exists
        otpRepository.deleteByEmail(request.getEmail());

        OtpVerification otp = OtpVerification.builder()
                .email(request.getEmail())
                .otpCode(otpCode)
                .expiresAt(LocalDateTime.now().plusSeconds(otpExpiration / 1000))
                .verified(false)
                .createdAt(LocalDateTime.now())
                .build();

        otpRepository.save(otp);
        emailService.sendOtpEmail(request.getEmail(), otpCode);

        return AuthDto.MessageResponse.builder()
                .message("Password reset OTP sent to your email")
                .build();
    }

    @Transactional
    public AuthDto.AuthResponse refresh(AuthDto.RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(request.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token has expired");
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtUtil.generateToken(user);

        return AuthDto.AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken())
                .tokenType("Bearer")
                .expiresIn(jwtExpiration)
                .user(UserDto.fromEntity(user))
                .build();
    }

    @Transactional
    public AuthDto.MessageResponse logout(Long userId) {
        refreshTokenRepository.deleteByUser_UserId(userId);
        return AuthDto.MessageResponse.builder()
                .message("Logged out successfully")
                .build();
    }

    private String generateOtpCode() {
        Random random = new Random();
        return "%06d".formatted(random.nextInt(999999));
    }
}