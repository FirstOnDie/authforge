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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin Management", description = "Endpoints for administrative actions (requires ADMIN role)")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final UserService userService;
    private final FeatureFlags featureFlags;

    public AdminController(UserService userService, FeatureFlags featureFlags) {
        this.userService = userService;
        this.featureFlags = featureFlags;
    }

    @Operation(summary = "Get all users", description = "Returns a list of all registered users.")
    @ApiResponse(responseCode = "200", description = "List of users returned successfully")
    @ApiResponse(responseCode = "403", description = "Access denied (requires ADMIN role)")
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

    @Operation(summary = "Change user role", description = "Updates the role (USER/ADMIN) of a specific user.")
    @ApiResponse(responseCode = "200", description = "User role updated successfully")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @ApiResponse(responseCode = "404", description = "User not found")
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

    @Operation(summary = "Get feature flags", description = "Returns the current state of system feature flags.")
    @ApiResponse(responseCode = "200", description = "Feature flags returned successfully")
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
