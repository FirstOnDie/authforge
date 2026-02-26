package com.authforge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TwoFactorSetupResponse {
    @Schema(description = "Base32 encoded TOTP secret", example = "JBSWY3DPEHPK3PXP")
    private String secret;
    @Schema(description = "URI for generating a QR Code in an authenticator app", example = "otpauth://totp/AuthForge:admin@authforge.local?secret=JBSWY3DPEHPK3PXP&issuer=AuthForge")
    private String qrUri;
}
