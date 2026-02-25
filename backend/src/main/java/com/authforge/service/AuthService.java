package com.authforge.service;

import com.authforge.dto.*;
import com.authforge.model.RefreshToken;
import com.authforge.model.Role;
import com.authforge.model.User;
import com.authforge.repository.UserRepository;
import com.authforge.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Authentication Service — the main brain of AuthForge.
 *
 * Handles:
 * - User registration (with BCrypt hashing)
 * - Login (authenticate + issue tokens)
 * - Token refresh (rotate refresh token + new access token)
 * - Logout (invalidate refresh tokens)
 * - Password recovery (generate reset token, apply new password)
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenService refreshTokenService,
            AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.authenticationManager = authenticationManager;
    }

    // ────────────────────────────────────────────
    // REGISTER
    // ────────────────────────────────────────────

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }

        // Create user with hashed password
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .enabled(true)
                .build();

        user = userRepository.save(user);
        log.info("✅ User registered: {}", user.getEmail());

        // Generate tokens
        return generateAuthResponse(user);
    }

    // ────────────────────────────────────────────
    // LOGIN
    // ────────────────────────────────────────────

    public AuthResponse login(LoginRequest request) {
        // Authenticate via Spring Security (validates password)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("✅ User logged in: {}", user.getEmail());

        return generateAuthResponse(user);
    }

    // ────────────────────────────────────────────
    // REFRESH TOKEN
    // ────────────────────────────────────────────

    @Transactional
    public AuthResponse refreshToken(TokenRefreshRequest request) {
        RefreshToken refreshToken = refreshTokenService
                .findByToken(request.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        // Verify it hasn't expired
        refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();

        // Generate new tokens (rotation: old refresh token is replaced)
        return generateAuthResponse(user);
    }

    // ────────────────────────────────────────────
    // LOGOUT
    // ────────────────────────────────────────────

    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        refreshTokenService.deleteByUser(user);
        log.info("✅ User logged out: {}", email);
    }

    // ────────────────────────────────────────────
    // FORGOT PASSWORD
    // ────────────────────────────────────────────

    @Transactional
    public String forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        // Generate a reset token
        String resetToken = UUID.randomUUID().toString();
        user.setVerificationToken(resetToken);
        userRepository.save(user);

        // In production, send this via email.
        // For this starter kit, we log it to the console.
        log.info("🔑 Password reset token for {}: {}", email, resetToken);

        return resetToken;
    }

    // ────────────────────────────────────────────
    // RESET PASSWORD
    // ────────────────────────────────────────────

    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        User user = userRepository.findByVerificationToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setVerificationToken(null); // Invalidate the token
        userRepository.save(user);

        log.info("✅ Password reset for: {}", user.getEmail());
    }

    // ────────────────────────────────────────────
    // HELPER: Build AuthResponse with tokens
    // ────────────────────────────────────────────

    private AuthResponse generateAuthResponse(User user) {
        // Build a UserDetails object for JWT generation
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();

        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .user(AuthResponse.UserDto.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .build())
                .build();
    }
}
