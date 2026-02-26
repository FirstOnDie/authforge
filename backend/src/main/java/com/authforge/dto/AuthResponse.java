package com.authforge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class AuthResponse {
    @Schema(description = "JWT Access Token", example = "eyJhbGciOiJIUzI1NiIsInR5c...")
    private String accessToken;
    @Schema(description = "JWT Refresh Token", example = "eyJhbGciOiJIUzI1NiIsInR5c...")
    private String refreshToken;
    @Schema(description = "Token type, typically Bearer", example = "Bearer")
    private String tokenType;
    @Schema(description = "Time in milliseconds until the access token expires", example = "900000")
    private long expiresIn;
    @Schema(description = "User profile information")
    private UserDto user;
    @Schema(description = "Flag indicating if a 2FA code is needed to finish login", example = "false")
    @Builder.Default
    private boolean requiresTwoFactor = false;
    @Schema(description = "Flag indicating if the user still needs to verify their email", example = "false")
    @Builder.Default
    private boolean requiresEmailVerification = false;

    @Data
    @AllArgsConstructor
    @Builder
    public static class UserDto {
        @Schema(description = "Unique user identifier", example = "1")
        private Long id;
        @Schema(description = "User's full name", example = "John Doe")
        private String name;
        @Schema(description = "User's email address", example = "john.doe@example.com")
        private String email;
        @Schema(description = "Assigned user role", example = "ROLE_USER")
        private String role;
        @Schema(description = "Status of Two-Factor Authentication for the user", example = "false")
        @Builder.Default
        private boolean twoFactorEnabled = false;
    }
}
