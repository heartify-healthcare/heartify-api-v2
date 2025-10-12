package com.healthcare.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDto {

    public static class RegisterRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;

        @NotBlank(message = "Full name is required")
        private String fullName;

        private String phoneNumber;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }
    }

    public static class VerifyOtpRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "OTP code is required")
        private String otpCode;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getOtpCode() {
            return otpCode;
        }

        public void setOtpCode(String otpCode) {
            this.otpCode = otpCode;
        }
    }

    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class ForgotPasswordRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    public static class RefreshTokenRequest {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }

    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
        private Long expiresIn;
        private UserDto user;

        private AuthResponse() {
            // Private constructor for Builder pattern
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public String getTokenType() {
            return tokenType;
        }

        public Long getExpiresIn() {
            return expiresIn;
        }

        public UserDto getUser() {
            return user;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final AuthResponse authResponse;

            private Builder() {
                authResponse = new AuthResponse();
            }

            public Builder accessToken(String accessToken) {
                authResponse.accessToken = accessToken;
                return this;
            }

            public Builder refreshToken(String refreshToken) {
                authResponse.refreshToken = refreshToken;
                return this;
            }

            public Builder tokenType(String tokenType) {
                authResponse.tokenType = tokenType;
                return this;
            }

            public Builder expiresIn(Long expiresIn) {
                authResponse.expiresIn = expiresIn;
                return this;
            }

            public Builder user(UserDto user) {
                authResponse.user = user;
                return this;
            }

            public AuthResponse build() {
                return authResponse;
            }
        }
    }

    public static class MessageResponse {
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final MessageResponse messageResponse;

            private Builder() {
                messageResponse = new MessageResponse();
            }

            public Builder message(String message) {
                messageResponse.message = message;
                return this;
            }

            public MessageResponse build() {
                return messageResponse;
            }
        }
    }
}