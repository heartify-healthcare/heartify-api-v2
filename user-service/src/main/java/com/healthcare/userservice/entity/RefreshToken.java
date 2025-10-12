package com.healthcare.userservice.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private RefreshToken() {
        // Private constructor for Builder pattern
    }
    
    @Column(nullable = false, unique = true)
    private String token;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(nullable = false)
    private Boolean revoked = false;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getRevoked() {
        return revoked;
    }

    public void setRevoked(Boolean revoked) {
        this.revoked = revoked;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final RefreshToken refreshToken;

        private Builder() {
            refreshToken = new RefreshToken();
        }

        public Builder token(String token) {
            refreshToken.token = token;
            return this;
        }

        public Builder user(User user) {
            refreshToken.user = user;
            return this;
        }

        public Builder expiresAt(LocalDateTime expiresAt) {
            refreshToken.expiresAt = expiresAt;
            return this;
        }

        public Builder revoked(Boolean revoked) {
            refreshToken.revoked = revoked;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            refreshToken.createdAt = createdAt;
            return this;
        }

        public RefreshToken build() {
            return refreshToken;
        }
    }
}