package com.authforge.controller;

import com.authforge.dto.*;
import com.authforge.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints for user registration, login, and token management")
public class AuthController {

    private static final String MESSAGE_KEY = "message";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register a new user", description = "Creates a new user account with the given details.")
    @ApiResponse(responseCode = "201", description = "User successfully registered")
    @ApiResponse(responseCode = "400", description = "Invalid request payload or email already exists")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Login to the application", description = "Authenticates a user and returns an access token and refresh token, or notifies if 2FA is required.")
    @ApiResponse(responseCode = "200", description = "Successful login or 2FA required")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Verify Two-Factor Authentication code", description = "Completes the login process by verifying a TOTP code.")
    @ApiResponse(responseCode = "200", description = "Code verified, tokens issued")
    @ApiResponse(responseCode = "400", description = "Invalid or expired code")
    @PostMapping("/2fa/verify")
    public ResponseEntity<AuthResponse> verifyTwoFactor(
            @Valid @RequestBody TwoFactorLoginRequest request) {
        AuthResponse response = authService.verifyTwoFactor(
                request.getEmail(), request.getCode());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Verify email address", description = "Verifies a user's email address using a token sent during registration.")
    @ApiResponse(responseCode = "200", description = "Email verified successfully")
    @ApiResponse(responseCode = "400", description = "Invalid or expired verification token")
    @GetMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(Map.of(MESSAGE_KEY, "Email verified successfully! You can now log in."));
    }

    @Operation(summary = "Refresh access token", description = "Exchanges a valid refresh token for a new access token.")
    @ApiResponse(responseCode = "200", description = "New tokens generated")
    @ApiResponse(responseCode = "403", description = "Invalid or expired refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Logout the current user", description = "Invalidates the current session and refresh token.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Logged out successfully")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(Authentication authentication) {
        authService.logout(authentication.getName());
        return ResponseEntity.ok(Map.of(MESSAGE_KEY, "Logged out successfully"));
    }

    @Operation(summary = "Initiate password recovery", description = "Sends a password reset link to the given email if it exists.")
    @ApiResponse(responseCode = "200", description = "Reset link sent (if email exists)")
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        authService.forgotPassword(email);
        return ResponseEntity.ok(Map.of(MESSAGE_KEY, "If the email exists, a reset link has been sent."));
    }

    @Operation(summary = "Complete password reset", description = "Resets the user's password using the token sent to their email.")
    @ApiResponse(responseCode = "200", description = "Password reset successfully")
    @ApiResponse(responseCode = "400", description = "Invalid or expired reset token, or weak password")
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of(MESSAGE_KEY, "Password reset successfully"));
    }
}
