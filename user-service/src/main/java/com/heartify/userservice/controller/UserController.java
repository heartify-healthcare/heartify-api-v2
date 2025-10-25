package com.heartify.userservice.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.heartify.userservice.dto.AuthDto;
import com.heartify.userservice.dto.UserDto;
import com.heartify.userservice.service.UserService;

import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // POST /users - Create user (Admin only)
    @PostMapping
    public ResponseEntity<?> createUser(
            @Valid @RequestBody UserDto.UserCreateRequest request,
            @RequestHeader("X-User-Role") String role) {
        
        if (!"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AuthDto.MessageResponse.builder()
                            .message("Access denied. Admin privileges required.")
                            .build());
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    // GET /users - List all users (Admin only)
    @GetMapping
    public ResponseEntity<?> listUsers(@RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AuthDto.MessageResponse.builder()
                            .message("Access denied. Admin privileges required.")
                            .build());
        }
        
        return ResponseEntity.ok(userService.listUsers());
    }

    // GET /users/{id} - Get user by ID (Users can view own, admins can view any)
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String currentUserIdStr,
            @RequestHeader("X-User-Role") String role) {
        
        UUID currentUserId = UUID.fromString(currentUserIdStr);
        
        if (!"ADMIN".equalsIgnoreCase(role) && !currentUserId.equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AuthDto.MessageResponse.builder()
                            .message("Access denied. You can only view your own profile.")
                            .build());
        }
        
        return ResponseEntity.ok(userService.getUserById(id));
    }

    // GET /users/profile - Get current user's profile
    @GetMapping("/profile")
    public ResponseEntity<UserDto> getCurrentUserProfile(@RequestHeader("X-User-Id") String userIdStr) {
        UUID userId = UUID.fromString(userIdStr);
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    // PATCH /users/{id} - Update user (Users can update own, admins can update any)
    @PatchMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UserDto.UpdateUserRequest request,
            @RequestHeader("X-User-Id") String currentUserIdStr,
            @RequestHeader("X-User-Role") String role) {
        
        UUID currentUserId = UUID.fromString(currentUserIdStr);
        
        if (!"ADMIN".equalsIgnoreCase(role) && !currentUserId.equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AuthDto.MessageResponse.builder()
                            .message("Access denied. You can only update your own profile.")
                            .build());
        }
        
        return ResponseEntity.ok(userService.updateUser(id, request, role));
    }

    // PATCH /users/profile - Update current user's profile
    @PatchMapping("/profile")
    public ResponseEntity<UserDto> updateCurrentUserProfile(
            @Valid @RequestBody UserDto.UpdateUserRequest request,
            @RequestHeader("X-User-Id") String userIdStr,
            @RequestHeader("X-User-Role") String role) {
        
        UUID userId = UUID.fromString(userIdStr);
        return ResponseEntity.ok(userService.updateUser(userId, request, role));
    }

    // PATCH /users/profile/health - Update current user's health info
    @PatchMapping("/profile/health")
    public ResponseEntity<UserDto> updateCurrentUserHealth(
            @Valid @RequestBody UserDto.UserHealthUpdateRequest request,
            @RequestHeader("X-User-Id") String userIdStr) {
        
        UUID userId = UUID.fromString(userIdStr);
        return ResponseEntity.ok(userService.updateUserHealth(userId, request));
    }

    // PUT /users/change-password - Change current user's password
    @PutMapping("/change-password")
    public ResponseEntity<AuthDto.MessageResponse> changePassword(
            @Valid @RequestBody UserDto.ChangePasswordRequest request,
            @RequestHeader("X-User-Id") String userIdStr) {
        
        UUID userId = UUID.fromString(userIdStr);
        userService.changePassword(userId, request);
        
        return ResponseEntity.ok(AuthDto.MessageResponse.builder()
                .message("Password changed successfully")
                .build());
    }

    // DELETE /users/{id} - Delete user (Admin only)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String currentUserIdStr,
            @RequestHeader("X-User-Role") String role) {
        
        UUID currentUserId = UUID.fromString(currentUserIdStr);
        
        if (!"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AuthDto.MessageResponse.builder()
                            .message("Access denied. Admin privileges required.")
                            .build());
        }
        
        // Prevent self-deletion
        if (currentUserId.equals(id)) {
            throw new RuntimeException("You cannot delete your own account");
        }
        
        userService.deleteUser(id);
        
        return ResponseEntity.ok(AuthDto.MessageResponse.builder()
                .message("User deleted successfully")
                .build());
    }
}