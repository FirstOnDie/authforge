package com.authforge.controller;

import com.authforge.dto.AuthResponse;
import com.authforge.model.User;
import com.authforge.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin Controller — requires ADMIN role.
 *
 * Only users with Role.ADMIN can access these endpoints
 * (enforced at SecurityConfig level).
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /api/admin/users
     * List all users in the system.
     */
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

    /**
     * PUT /api/admin/users/{id}/role
     * Change a user's role.
     *
     * Body: { "role": "ADMIN" } or { "role": "USER" }
     */
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
}
