package com.healthcare.userservice.dto;

import com.healthcare.userservice.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String phonenumber;
    private Boolean isVerified;
    private String role;
    private LocalDateTime createdAt;

    // Health fields
    private LocalDate dob;
    private Integer sex;
    private Integer cp;
    private Integer trestbps;
    private Integer exang;

    private UserDto() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final UserDto userDto;

        private Builder() {
            userDto = new UserDto();
        }

        public Builder id(Long id) {
            userDto.id = id;
            return this;
        }

        public Builder username(String username) {
            userDto.username = username;
            return this;
        }

        public Builder email(String email) {
            userDto.email = email;
            return this;
        }

        public Builder phonenumber(String phonenumber) {
            userDto.phonenumber = phonenumber;
            return this;
        }

        public Builder role(String role) {
            userDto.role = role;
            return this;
        }

        public Builder isVerified(Boolean isVerified) {
            userDto.isVerified = isVerified;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            userDto.createdAt = createdAt;
            return this;
        }

        public Builder dob(LocalDate dob) {
            userDto.dob = dob;
            return this;
        }

        public Builder sex(Integer sex) {
            userDto.sex = sex;
            return this;
        }

        public Builder cp(Integer cp) {
            userDto.cp = cp;
            return this;
        }

        public Builder trestbps(Integer trestbps) {
            userDto.trestbps = trestbps;
            return this;
        }

        public Builder exang(Integer exang) {
            userDto.exang = exang;
            return this;
        }

        public UserDto build() {
            return userDto;
        }
    }

    public static UserDto fromEntity(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phonenumber(user.getPhonenumber())
                .role(user.getRole().name())
                .isVerified(user.getIsVerified())
                .createdAt(user.getCreatedAt())
                .dob(user.getDob())
                .sex(user.getSex())
                .cp(user.getCp())
                .trestbps(user.getTrestbps())
                .exang(user.getExang())
                .build();
    }

    // NEW: UserCreateRequest (for admin creating users)
    public static class UserCreateRequest {
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

        private String role = "user"; // default

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

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

    // NEW: UserUpdateRequest
    public static class UpdateUserRequest {
        @Size(min = 3, message = "Username must be at least 3 characters")
        private String username;

        @Email(message = "Invalid email format")
        private String email;

        private String phonenumber;

        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        private Boolean isVerified;
        private String role;

        // Health fields
        private LocalDate dob;
        private Integer sex;
        private Integer cp;
        private Integer trestbps;
        private Integer exang;

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

        public Boolean getIsVerified() {
            return isVerified;
        }

        public void setIsVerified(Boolean isVerified) {
            this.isVerified = isVerified;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public LocalDate getDob() {
            return dob;
        }

        public void setDob(LocalDate dob) {
            this.dob = dob;
        }

        public Integer getSex() {
            return sex;
        }

        public void setSex(Integer sex) {
            this.sex = sex;
        }

        public Integer getCp() {
            return cp;
        }

        public void setCp(Integer cp) {
            this.cp = cp;
        }

        public Integer getTrestbps() {
            return trestbps;
        }

        public void setTrestbps(Integer trestbps) {
            this.trestbps = trestbps;
        }

        public Integer getExang() {
            return exang;
        }

        public void setExang(Integer exang) {
            this.exang = exang;
        }
    }

    // NEW: UserHealthUpdateRequest
    public static class UserHealthUpdateRequest {
        private LocalDate dob;
        private Integer sex;
        private Integer cp;
        private Integer trestbps;
        private Integer exang;

        public LocalDate getDob() {
            return dob;
        }

        public void setDob(LocalDate dob) {
            this.dob = dob;
        }

        public Integer getSex() {
            return sex;
        }

        public void setSex(Integer sex) {
            this.sex = sex;
        }

        public Integer getCp() {
            return cp;
        }

        public void setCp(Integer cp) {
            this.cp = cp;
        }

        public Integer getTrestbps() {
            return trestbps;
        }

        public void setTrestbps(Integer trestbps) {
            this.trestbps = trestbps;
        }

        public Integer getExang() {
            return exang;
        }

        public void setExang(Integer exang) {
            this.exang = exang;
        }
    }

    // NEW: ChangePasswordRequest
    public static class ChangePasswordRequest {
        @NotBlank(message = "Current password is required")
        private String currentPassword;

        @NotBlank(message = "New password is required")
        @Size(min = 6, message = "New password must be at least 6 characters")
        private String newPassword;

        public String getCurrentPassword() {
            return currentPassword;
        }

        public void setCurrentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Boolean getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(Boolean isVerified) {
        this.isVerified = isVerified;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public Integer getSex() {
        return sex;
    }

    public void setSex(Integer sex) {
        this.sex = sex;
    }

    public Integer getCp() {
        return cp;
    }

    public void setCp(Integer cp) {
        this.cp = cp;
    }

    public Integer getTrestbps() {
        return trestbps;
    }

    public void setTrestbps(Integer trestbps) {
        this.trestbps = trestbps;
    }

    public Integer getExang() {
        return exang;
    }

    public void setExang(Integer exang) {
        this.exang = exang;
    }
}