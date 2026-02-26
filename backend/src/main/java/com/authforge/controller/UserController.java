package com.authforge.controller;

import com.authforge.dto.AuthResponse;
import com.authforge.model.User;
import com.authforge.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Profile", description = "Endpoints for managing user accounts")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Get current user profile", description = "Returns the profile information of the currently authenticated user.")
    @ApiResponse(responseCode = "200", description = "User profile returned successfully")
    @GetMapping("/me")
    public ResponseEntity<AuthResponse.UserDto> getCurrentUser(Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());

        AuthResponse.UserDto dto = AuthResponse.UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();

        return ResponseEntity.ok(dto);
    }
}
