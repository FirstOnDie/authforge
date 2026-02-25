package com.authforge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private UserDto user;
    @Builder.Default
    private boolean requiresTwoFactor = false;

    @Data
    @AllArgsConstructor
    @Builder
    public static class UserDto {
        private Long id;
        private String name;
        private String email;
        private String role;
        @Builder.Default
        private boolean twoFactorEnabled = false;
    }
}
