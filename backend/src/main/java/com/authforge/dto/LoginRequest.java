package com.authforge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @Schema(description = "User's email address", example = "admin@authforge.local")
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "User's password", example = "admin123")
    @NotBlank(message = "Password is required")
    private String password;
}
