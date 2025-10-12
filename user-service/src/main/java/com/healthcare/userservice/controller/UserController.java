package com.healthcare.userservice.controller;

import com.healthcare.userservice.dto.UserDto;
import com.healthcare.userservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(userService.getCurrentUser(userId));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserDto> updateCurrentUser(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody UserDto.UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateCurrentUser(userId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") String role) {
        // Only CLINICIAN and ADMIN can view other users
        if (!role.equals("CLINICIAN") && !role.equals("ADMIN")) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @RequestHeader("X-User-Role") String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String roleFilter) {
        // Only ADMIN can view all users
        if (!role.equals("ADMIN")) {
            return ResponseEntity.status(403).build();
        }
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(userService.getAllUsers(pageable, roleFilter));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") String role) {
        // Only ADMIN can delete users
        if (!role.equals("ADMIN")) {
            return ResponseEntity.status(403).build();
        }
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}