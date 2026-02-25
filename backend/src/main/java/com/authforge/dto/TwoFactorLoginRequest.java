package com.authforge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TwoFactorLoginRequest {

    @NotBlank
    private String email;

    @NotBlank
    private String code;
}
