package com.heartify.userservice.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.heartify.userservice.dto.AuthDto;
import com.heartify.userservice.dto.UserDto;
import com.heartify.userservice.entity.OtpVerification;
import com.heartify.userservice.entity.User;
import com.heartify.userservice.repository.OtpVerificationRepository;
import com.heartify.userservice.repository.UserRepository;
import com.heartify.userservice.util.JwtUtil;

import java.time.Instant;
import java.util.Random;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OtpVerificationRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    @Value("${otp.expiration}")
    private Long otpExpiration; // 300 seconds = 5 minutes

    public AuthService(UserRepository userRepository,
                       OtpVerificationRepository otpRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
    }

    @Transactional
    public AuthDto.MessageResponse register(AuthDto.RegisterRequest request) {
        // Check if username exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        
        // Check if email exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        
        // Check if phone number exists
        if (request.getPhonenumber() != null && !request.getPhonenumber().isEmpty()) {
            if (userRepository.existsByPhonenumber(request.getPhonenumber())) {
                throw new RuntimeException("Phone number already exists");
            }
        }

        // Create user (unverified)
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .phonenumber(request.getPhonenumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.UserRole.USER)
                .isVerified(false)
                .status(User.UserStatus.ACTIVE)
                .build();

        user = userRepository.save(user);

        // Send OTP
        sendVerificationOtp(user);

        return AuthDto.MessageResponse.builder()
                .message("User registered successfully. Please check your email for verification code.")
                .build();
    }

    @Transactional
    public AuthDto.MessageResponse requestVerify(AuthDto.RequestVerifyRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getIsVerified()) {
            throw new RuntimeException("User is already verified");
        }

        sendVerificationOtp(user);

        return AuthDto.MessageResponse.builder()
                .message("Verification code sent to your email")
                .build();
    }

    @Transactional
    public AuthDto.MessageResponse verifyOtp(AuthDto.VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getIsVerified()) {
            throw new RuntimeException("User is already verified");
        }

        // Find valid OTP
        OtpVerification otp = otpRepository.findByUser_IdAndOtpCodeAndOtpUsedFalse(user.getId(), request.getOtpCode())
                .orElseThrow(() -> new RuntimeException("Invalid or expired OTP"));

        // Check if OTP is expired
        long currentTime = Instant.now().getEpochSecond();
        if (otp.getExpiredTime() < currentTime) {
            throw new RuntimeException("OTP has expired");
        }

        // Mark OTP as used
        otp.setOtpUsed(true);
        otpRepository.save(otp);

        // Mark user as verified
        user.setIsVerified(true);
        userRepository.save(user);

        return AuthDto.MessageResponse.builder()
                .message("Account verified successfully")
                .build();
    }

    @Transactional
    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        if (!user.getIsVerified()) {
            throw new RuntimeException("Please verify your account before logging in");
        }

        if (user.getStatus() == User.UserStatus.INACTIVE) {
            throw new RuntimeException("Account is inactive");
        }

        String accessToken = jwtUtil.generateToken(user);

        return AuthDto.AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("bearer")
                .user(UserDto.fromEntity(user))
                .build();
    }

    @Transactional
    public AuthDto.MessageResponse recoverPassword(AuthDto.RecoverPasswordRequest request) {
        // Step 1: Find user by username
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Username not found"));

        // Step 2: Validate email
        if (!user.getEmail().equals(request.getEmail())) {
            throw new RuntimeException("Email does not match");
        }

        // Step 3: Validate phone number
        if (!user.getPhonenumber().equals(request.getPhoneNumber())) {
            throw new RuntimeException("Phone number does not match");
        }

        // Generate new password (8 chars: uppercase + lowercase + digits)
        String newPassword = generateRandomPassword();

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Send recovery email with new password
        emailService.sendPasswordRecoveryEmail(user.getEmail(), user.getUsername(), newPassword);

        return AuthDto.MessageResponse.builder()
                .message("Password reset successfully. Please check your email for the new password.")
                .build();
    }

    private void sendVerificationOtp(User user) {
        // Invalidate old OTPs
        otpRepository.invalidateUserOtps(user.getId());

        // Generate 6-digit OTP
        String otpCode = String.format("%06d", new Random().nextInt(999999));
        
        // Calculate expiration time (current time + 5 minutes)
        long expiredTime = Instant.now().getEpochSecond() + otpExpiration;

        // Save OTP
        OtpVerification otp = OtpVerification.builder()
                .user(user)
                .otpCode(otpCode)
                .expiredTime(expiredTime)
                .otpUsed(false)
                .build();

        otpRepository.save(otp);

        // Send email
        emailService.sendOtpEmail(user.getEmail(), user.getUsername(), otpCode);
    }

    private String generateRandomPassword() {
        String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowercase = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        
        Random random = new Random();
        StringBuilder password = new StringBuilder();
        
        // At least one of each
        password.append(uppercase.charAt(random.nextInt(uppercase.length())));
        password.append(lowercase.charAt(random.nextInt(lowercase.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        
        // Fill remaining 5 characters
        String allChars = uppercase + lowercase + digits;
        for (int i = 0; i < 5; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }
        
        // Shuffle
        char[] chars = password.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        
        return new String(chars);
    }
}