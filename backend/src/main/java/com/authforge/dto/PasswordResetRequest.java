package com.authforge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordResetRequest {

    @Schema(description = "Reset token delivered via email", example = "abc123xyz456")
    @NotBlank
    private String token;

    @Schema(description = "New strong password", example = "NewSecureP@ss123!")
    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;
}
