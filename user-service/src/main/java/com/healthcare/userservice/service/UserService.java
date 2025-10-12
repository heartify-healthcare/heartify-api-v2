package com.healthcare.userservice.service;

import com.healthcare.userservice.dto.UserDto;
import com.healthcare.userservice.entity.User;
import com.healthcare.userservice.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserDto getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return UserDto.fromEntity(user);
    }

    @Transactional
    public UserDto updateCurrentUser(Long userId, UserDto.UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        user = userRepository.save(user);
        return UserDto.fromEntity(user);
    }

    public UserDto getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return UserDto.fromEntity(user);
    }

    public Page<UserDto> getAllUsers(Pageable pageable, String role) {
        Page<User> users;
        if (role != null && !role.isEmpty()) {
            User.UserRole userRole = User.UserRole.valueOf(role.toUpperCase());
            users = userRepository.findByRole(userRole, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }
        return users.map(UserDto::fromEntity);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(User.UserStatus.INACTIVE);
        userRepository.save(user);
    }
}