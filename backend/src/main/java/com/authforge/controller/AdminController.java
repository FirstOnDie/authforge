package com.authforge.controller;

import com.authforge.config.FeatureFlags;
import com.authforge.dto.AuthResponse;
import com.authforge.model.User;
import com.authforge.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final FeatureFlags featureFlags;

    public AdminController(UserService userService, FeatureFlags featureFlags) {
        this.userService = userService;
        this.featureFlags = featureFlags;
    }

    @GetMapping("/users")
    public ResponseEntity<List<AuthResponse.UserDto>> getAllUsers() {
        List<AuthResponse.UserDto> users = userService.getAllUsers().stream()
                .map(user -> AuthResponse.UserDto.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .build())
                .toList();

        return ResponseEntity.ok(users);
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<AuthResponse.UserDto> changeUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        User user = userService.changeRole(id, body.get("role"));

        AuthResponse.UserDto dto = AuthResponse.UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/features")
    public ResponseEntity<Map<String, Boolean>> getFeatures() {
        Map<String, Boolean> features = new LinkedHashMap<>();
        features.put("oauth2", featureFlags.isOauth2());
        features.put("twoFactor", featureFlags.isTwoFactor());
        features.put("rateLimiting", featureFlags.isRateLimiting());
        features.put("emailVerification", featureFlags.isEmailVerification());
        return ResponseEntity.ok(features);
    }
}
