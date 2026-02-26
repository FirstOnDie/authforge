package com.authforge.controller;

import com.authforge.config.FeatureFlags;
import com.authforge.dto.TwoFactorSetupResponse;
import com.authforge.model.User;
import com.authforge.service.TotpService;
import com.authforge.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;

@RestController
@RequestMapping("/api/2fa")
@Tag(name = "Two-Factor Authentication", description = "Endpoints for setting up and managing TOTP 2FA")
@SecurityRequirement(name = "bearerAuth")
public class TwoFactorController {

    private static final String ERROR_KEY = "error";
    private static final String TWO_FACTOR_DISABLED_MSG = "Two-factor authentication is disabled";

    private final TotpService totpService;
    private final UserService userService;
    private final FeatureFlags featureFlags;

    public TwoFactorController(TotpService totpService, UserService userService, FeatureFlags featureFlags) {
        this.totpService = totpService;
        this.userService = userService;
        this.featureFlags = featureFlags;
    }

    @Operation(summary = "Setup 2FA", description = "Generates a TOTP secret and QR code URI for the authenticated user.")
    @ApiResponse(responseCode = "200", description = "Setup details returned successfully")
    @ApiResponse(responseCode = "404", description = "2FA feature is disabled globally")
    @PostMapping("/setup")
    public ResponseEntity<Object> setup(Authentication authentication) {
        if (!featureFlags.isTwoFactor()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(ERROR_KEY, TWO_FACTOR_DISABLED_MSG));
        }
        User user = userService.getUserByEmail(authentication.getName());
        String secret = totpService.generateSecret();
        String qrUri = totpService.generateQrUri(secret, user.getEmail());
        return ResponseEntity.ok(new TwoFactorSetupResponse(secret, qrUri));
    }

    @Operation(summary = "Enable 2FA", description = "Verifies a code against the generated secret to enable 2FA for the user.")
    @ApiResponse(responseCode = "200", description = "2FA enabled successfully")
    @ApiResponse(responseCode = "400", description = "Invalid 2FA code")
    @PostMapping("/enable")
    public ResponseEntity<Object> enable(
            Authentication authentication,
            @RequestBody Map<String, String> body) {

        if (!featureFlags.isTwoFactor()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(ERROR_KEY, TWO_FACTOR_DISABLED_MSG));
        }

        User user = userService.getUserByEmail(authentication.getName());
        String secret = body.get("secret");
        String code = body.get("code");

        if (!totpService.verifyCode(secret, code)) {
            throw new com.authforge.exception.BadRequestException("Invalid 2FA code");
        }

        userService.enableTwoFactor(user.getId(), secret);
        return ResponseEntity.ok(Map.of("message", "Two-factor authentication enabled"));
    }

    @Operation(summary = "Disable 2FA", description = "Disables 2FA for the authenticated user.")
    @ApiResponse(responseCode = "200", description = "2FA disabled successfully")
    @PostMapping("/disable")
    public ResponseEntity<Object> disable(Authentication authentication) {
        if (!featureFlags.isTwoFactor()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(ERROR_KEY, TWO_FACTOR_DISABLED_MSG));
        }
        User user = userService.getUserByEmail(authentication.getName());
        userService.disableTwoFactor(user.getId());
        return ResponseEntity.ok(Map.of("message", "Two-factor authentication disabled"));
    }
}
