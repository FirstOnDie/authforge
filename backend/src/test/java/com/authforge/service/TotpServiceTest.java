package com.authforge.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TotpServiceTest {

    private final TotpService totpService = new TotpService();

    @Test
    void shouldGenerateSecret() {
        String secret = totpService.generateSecret();
        assertThat(secret).isNotBlank()
                .hasSizeGreaterThanOrEqualTo(16);
    }

    @Test
    void shouldGenerateQrUri() {
        String secret = totpService.generateSecret();
        String qrUri = totpService.generateQrUri(secret, "test@example.com");
        assertThat(qrUri).startsWith("otpauth://totp/AuthForge:")
                .contains(secret)
                .contains("test@example.com");
    }

    @Test
    void shouldRejectInvalidCode() {
        String secret = totpService.generateSecret();
        boolean result = totpService.verifyCode(secret, "000000");
        assertThat(result).isFalse();
    }
}
