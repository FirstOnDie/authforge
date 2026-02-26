package com.authforge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TokenRefreshRequest {

    @Schema(description = "The refresh token previously issued to the client", example = "eyJhbGciOiJIUzI1NiIsInR5c...")
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
