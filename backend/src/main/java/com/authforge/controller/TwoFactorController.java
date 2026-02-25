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

import java.util.Map;

@RestController
@RequestMapping("/api/2fa")
public class TwoFactorController {

    private final TotpService totpService;
    private final UserService userService;
    private final FeatureFlags featureFlags;

    public TwoFactorController(TotpService totpService, UserService userService, FeatureFlags featureFlags) {
        this.totpService = totpService;
        this.userService = userService;
        this.featureFlags = featureFlags;
    }

    @PostMapping("/setup")
    public ResponseEntity<?> setup(Authentication authentication) {
        if (!featureFlags.isTwoFactor()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Two-factor authentication is disabled"));
        }
        User user = userService.getUserByEmail(authentication.getName());
        String secret = totpService.generateSecret();
        String qrUri = totpService.generateQrUri(secret, user.getEmail());
        return ResponseEntity.ok(new TwoFactorSetupResponse(secret, qrUri));
    }

    @PostMapping("/enable")
    public ResponseEntity<?> enable(
            Authentication authentication,
            @RequestBody Map<String, String> body) {

        if (!featureFlags.isTwoFactor()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Two-factor authentication is disabled"));
        }

        User user = userService.getUserByEmail(authentication.getName());
        String secret = body.get("secret");
        String code = body.get("code");

        if (!totpService.verifyCode(secret, code)) {
            throw new RuntimeException("Invalid 2FA code");
        }

        userService.enableTwoFactor(user.getId(), secret);
        return ResponseEntity.ok(Map.of("message", "Two-factor authentication enabled"));
    }

    @PostMapping("/disable")
    public ResponseEntity<?> disable(Authentication authentication) {
        if (!featureFlags.isTwoFactor()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Two-factor authentication is disabled"));
        }
        User user = userService.getUserByEmail(authentication.getName());
        userService.disableTwoFactor(user.getId());
        return ResponseEntity.ok(Map.of("message", "Two-factor authentication disabled"));
    }
}
