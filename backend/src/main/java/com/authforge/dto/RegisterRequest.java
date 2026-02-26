package com.authforge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @Schema(description = "Full name of the user", example = "John Doe")
    @NotBlank(message = "Name is required")
    private String name;

    @Schema(description = "Valid email address", example = "john.doe@example.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "Strong password (minimum 8 characters)", example = "SecureP@ssw0rd")
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
