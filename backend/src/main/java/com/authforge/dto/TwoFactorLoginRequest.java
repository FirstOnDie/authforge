package com.authforge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TwoFactorLoginRequest {

    @Schema(description = "Email of the user attempting to login", example = "admin@authforge.local")
    @NotBlank
    private String email;

    @Schema(description = "6-digit TOTP code", example = "123456")
    @NotBlank
    private String code;
}
