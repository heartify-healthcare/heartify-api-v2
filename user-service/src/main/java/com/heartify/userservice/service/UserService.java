package com.heartify.userservice.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.heartify.userservice.dto.UserDto;
import com.heartify.userservice.entity.User;
import com.heartify.userservice.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserDto createUser(UserDto.UserCreateRequest request) {
        // Validation
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        if (request.getPhonenumber() != null && userRepository.existsByPhonenumber(request.getPhonenumber())) {
            throw new RuntimeException("Phone number already exists");
        }

        // Validate role
        User.UserRole role;
        try {
            role = User.UserRole.valueOf(request.getRole().toUpperCase());
        } catch (Exception e) {
            throw new RuntimeException("Invalid role. Must be 'user' or 'admin'");
        }

        // Create user (admin-created users are auto-verified)
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .phonenumber(request.getPhonenumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .isVerified(true)
                .status(User.UserStatus.ACTIVE)
                .build();

        user = userRepository.save(user);
        return UserDto.fromEntity(user);
    }

    public List<UserDto> listUsers() {
        return userRepository.findAll().stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
    }

    public UserDto getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return UserDto.fromEntity(user);
    }

    @Transactional
    public UserDto updateUser(Long userId, UserDto.UpdateUserRequest request, String currentUserRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate unique fields if they're being changed
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("Username already exists");
            }
            user.setUsername(request.getUsername());
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already exists");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getPhonenumber() != null && !request.getPhonenumber().equals(user.getPhonenumber())) {
            if (userRepository.existsByPhonenumber(request.getPhonenumber())) {
                throw new RuntimeException("Phone number already exists");
            }
            user.setPhonenumber(request.getPhonenumber());
        }

        // Password update
        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // Admin-only fields
        if ("admin".equalsIgnoreCase(currentUserRole)) {
            if (request.getIsVerified() != null) {
                user.setIsVerified(request.getIsVerified());
            }
            if (request.getRole() != null) {
                user.setRole(User.UserRole.valueOf(request.getRole().toUpperCase()));
            }
        }

        // Health fields (any user can update their own)
        if (request.getDob() != null) user.setDob(request.getDob());
        if (request.getSex() != null) user.setSex(request.getSex());
        if (request.getCp() != null) user.setCp(request.getCp());
        if (request.getTrestbps() != null) user.setTrestbps(request.getTrestbps());
        if (request.getExang() != null) user.setExang(request.getExang());

        user = userRepository.save(user);
        return UserDto.fromEntity(user);
    }

    @Transactional
    public UserDto updateUserHealth(Long userId, UserDto.UserHealthUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getDob() != null) user.setDob(request.getDob());
        if (request.getSex() != null) user.setSex(request.getSex());
        if (request.getCp() != null) user.setCp(request.getCp());
        if (request.getTrestbps() != null) user.setTrestbps(request.getTrestbps());
        if (request.getExang() != null) user.setExang(request.getExang());

        user = userRepository.save(user);
        return UserDto.fromEntity(user);
    }

    @Transactional
    public void changePassword(Long userId, UserDto.ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userRepository.delete(user);
    }
}