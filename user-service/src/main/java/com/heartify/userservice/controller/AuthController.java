package com.heartify.userservice.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.heartify.userservice.dto.AuthDto;
import com.heartify.userservice.dto.UserDto;
import com.heartify.userservice.repository.UserRepository;
import com.heartify.userservice.service.AuthService;
import com.heartify.userservice.service.UserService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthService authService, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDto.MessageResponse> register(@Valid @RequestBody AuthDto.RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/request-verify")
    public ResponseEntity<AuthDto.MessageResponse> requestVerify(@Valid @RequestBody AuthDto.RequestVerifyRequest request) {
        return ResponseEntity.ok(authService.requestVerify(request));
    }

    @PostMapping("/verify")
    public ResponseEntity<AuthDto.MessageResponse> verify(@Valid @RequestBody AuthDto.VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDto.AuthResponse> login(@Valid @RequestBody AuthDto.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/recover-password")
    public ResponseEntity<AuthDto.MessageResponse> recoverPassword(@Valid @RequestBody AuthDto.RecoverPasswordRequest request) {
        return ResponseEntity.ok(authService.recoverPassword(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@RequestHeader("X-User-Id") Long userId) {
        UserService userService = new UserService(userRepository, passwordEncoder);
        return ResponseEntity.ok(userService.getUserById(userId));
    }
}