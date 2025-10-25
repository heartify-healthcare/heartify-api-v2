package com.heartify.userservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.heartify.userservice.entity.User;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByPhonenumber(String phonenumber);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByPhonenumber(String phonenumber);
    Page<User> findByRole(User.UserRole role, Pageable pageable);
}