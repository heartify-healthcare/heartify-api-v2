package com.healthcare.userservice.controller;

import com.healthcare.userservice.dto.AuthDto;
import com.healthcare.userservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDto.MessageResponse> register(@Valid @RequestBody AuthDto.RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthDto.MessageResponse> verifyOtp(@Valid @RequestBody AuthDto.VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDto.AuthResponse> login(@Valid @RequestBody AuthDto.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<AuthDto.MessageResponse> forgotPassword(@Valid @RequestBody AuthDto.ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthDto.AuthResponse> refresh(@Valid @RequestBody AuthDto.RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthDto.MessageResponse> logout(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(authService.logout(userId));
    }
}