package com.quickkart.controller;

import com.quickkart.dto.AuthDtos;
import com.quickkart.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Registration, login, JWT refresh, password reset, and user profile")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new customer account")
    public ResponseEntity<AuthDtos.AuthResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and return access & refresh JWT tokens")
    public ResponseEntity<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh expired access token using refresh token")
    public ResponseEntity<AuthDtos.AuthResponse> refresh(@Valid @RequestBody AuthDtos.RefreshTokenRequest req) {
        return ResponseEntity.ok(authService.refresh(req));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset token")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody AuthDtos.ForgotPasswordRequest req) {
        authService.forgotPassword(req);
        return ResponseEntity.ok(Map.of("message", "If an account exists with this email, reset instructions have been sent."));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using token")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody AuthDtos.ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.ok(Map.of("message", "Your password has been updated. You can now log in."));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<AuthDtos.UserProfileDto> me() {
        return ResponseEntity.ok(authService.getMyProfile());
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<AuthDtos.UserProfileDto> updateProfile(@Valid @RequestBody AuthDtos.UpdateProfileRequest req) {
        return ResponseEntity.ok(authService.updateMyProfile(req));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change current user password")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody AuthDtos.ChangePasswordRequest req) {
        authService.changePassword(req);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully."));
    }
}
