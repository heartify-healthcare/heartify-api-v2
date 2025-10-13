package com.healthcare.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDto {

    // REGISTER REQUEST - updated to match Flask
    public static class RegisterRequest {
        @NotBlank(message = "Username is required")
        @Size(min = 3, message = "Username must be at least 3 characters")
        private String username;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        private String phonenumber;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhonenumber() {
            return phonenumber;
        }

        public void setPhonenumber(String phonenumber) {
            this.phonenumber = phonenumber;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    // NEW: Request Verify OTP (resend OTP)
    public static class RequestVerifyRequest {
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

    // VERIFY OTP REQUEST - updated
    public static class VerifyOtpRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "OTP code is required")
        @Size(min = 6, max = 6, message = "OTP must be 6 digits")
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

    // LOGIN REQUEST - updated to use username instead of email
    public static class LoginRequest {
        @NotBlank(message = "Username is required")
        private String username;

        @NotBlank(message = "Password is required")
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    // NEW: Recover Password Request
    public static class RecoverPasswordRequest {
        @NotBlank(message = "Username is required")
        private String username;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Phone number is required")
        private String phoneNumber;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }
    }

    // REMOVE RefreshTokenRequest class - not needed in Flask version

    // AUTH RESPONSE - updated
    public static class AuthResponse {
        private String accessToken;
        private String tokenType = "bearer";
        private UserDto user;

        private AuthResponse() {
            // Private constructor for Builder pattern
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getTokenType() {
            return tokenType;
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

            public Builder tokenType(String tokenType) {
                authResponse.tokenType = tokenType;
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

    // MESSAGE RESPONSE - same as before
    public static class MessageResponse {
        private String message;

        private MessageResponse() {
            // Private constructor for Builder pattern
        }

        public String getMessage() {
            return message;
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