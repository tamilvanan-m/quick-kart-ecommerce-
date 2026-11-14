package com.quickkart.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public class AuthDtos {

    public record RegisterRequest(
            @NotBlank(message = "Full name is required")
            @Size(min = 2, max = 120)
            String fullName,

            @NotBlank(message = "Email is required")
            @Email(message = "Valid email address is required")
            String email,

            @NotBlank(message = "Password is required")
            @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
            String password,

            @Size(max = 20)
            String phone
    ) {}

    public record LoginRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Valid email address is required")
            String email,

            @NotBlank(message = "Password is required")
            String password
    ) {}

    public record RefreshTokenRequest(
            @NotBlank(message = "Refresh token is required")
            String refreshToken
    ) {}

    public record ForgotPasswordRequest(
            @NotBlank(message = "Email is required")
            @Email
            String email
    ) {}

    public record ResetPasswordRequest(
            @NotBlank(message = "Reset token is required")
            String token,

            @NotBlank(message = "New password is required")
            @Size(min = 6, max = 100)
            String newPassword
    ) {}

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            UserProfileDto user
    ) {}

    public record UserProfileDto(
            Long id,
            String fullName,
            String email,
            String phone,
            String avatarUrl,
            Boolean active,
            List<String> roles,
            LocalDateTime createdAt
    ) {}

    public record UpdateProfileRequest(
            @NotBlank(message = "Full name is required")
            @Size(min = 2, max = 120)
            String fullName,

            @Size(max = 20)
            String phone,

            String avatarUrl
    ) {}

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 6, max = 100) String newPassword
    ) {}
}
