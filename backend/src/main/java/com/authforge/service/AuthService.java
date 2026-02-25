package com.authforge.service;

import com.authforge.config.FeatureFlags;
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

@Service
public class AuthService {

        private static final Logger log = LoggerFactory.getLogger(AuthService.class);

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtTokenProvider jwtTokenProvider;
        private final RefreshTokenService refreshTokenService;
        private final AuthenticationManager authenticationManager;
        private final TotpService totpService;
        private final EmailService emailService;
        private final FeatureFlags featureFlags;

        public AuthService(
                        UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        JwtTokenProvider jwtTokenProvider,
                        RefreshTokenService refreshTokenService,
                        AuthenticationManager authenticationManager,
                        TotpService totpService,
                        EmailService emailService,
                        FeatureFlags featureFlags) {
                this.userRepository = userRepository;
                this.passwordEncoder = passwordEncoder;
                this.jwtTokenProvider = jwtTokenProvider;
                this.refreshTokenService = refreshTokenService;
                this.authenticationManager = authenticationManager;
                this.totpService = totpService;
                this.emailService = emailService;
                this.featureFlags = featureFlags;
        }

        @Transactional
        public AuthResponse register(RegisterRequest request) {
                if (userRepository.existsByEmail(request.getEmail())) {
                        throw new RuntimeException("Email already registered: " + request.getEmail());
                }

                User user = User.builder()
                                .name(request.getName())
                                .email(request.getEmail())
                                .password(passwordEncoder.encode(request.getPassword()))
                                .role(Role.USER)
                                .enabled(true)
                                .emailVerified(!featureFlags.isEmailVerification())
                                .build();

                user = userRepository.save(user);
                log.info("User registered: {}", user.getEmail());

                if (featureFlags.isEmailVerification()) {
                        String token = UUID.randomUUID().toString();
                        user.setVerificationToken(token);
                        userRepository.save(user);
                        emailService.sendVerificationEmail(user.getEmail(), token);
                        log.info("Verification email sent to: {}", user.getEmail());

                        return AuthResponse.builder()
                                        .requiresEmailVerification(true)
                                        .user(AuthResponse.UserDto.builder()
                                                        .email(user.getEmail())
                                                        .name(user.getName())
                                                        .build())
                                        .build();
                }

                return generateAuthResponse(user);
        }

        public AuthResponse login(LoginRequest request) {
                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                request.getEmail(),
                                                request.getPassword()));

                User user = userRepository.findByEmail(request.getEmail())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                if (featureFlags.isEmailVerification() && !user.isEmailVerified()) {
                        throw new RuntimeException("Please verify your email before logging in");
                }

                if (featureFlags.isTwoFactor() && user.isTwoFactorEnabled()) {
                        log.info("2FA required for: {}", user.getEmail());
                        return AuthResponse.builder()
                                        .requiresTwoFactor(true)
                                        .user(AuthResponse.UserDto.builder()
                                                        .email(user.getEmail())
                                                        .build())
                                        .build();
                }

                log.info("User logged in: {}", user.getEmail());
                return generateAuthResponse(user);
        }

        public AuthResponse verifyTwoFactor(String email, String code) {
                if (!featureFlags.isTwoFactor()) {
                        throw new RuntimeException("Two-factor authentication is disabled");
                }

                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                if (!user.isTwoFactorEnabled() || user.getTwoFactorSecret() == null) {
                        throw new RuntimeException("Two-factor authentication is not enabled");
                }

                if (!totpService.verifyCode(user.getTwoFactorSecret(), code)) {
                        throw new RuntimeException("Invalid 2FA code");
                }

                log.info("2FA verified for: {}", user.getEmail());
                return generateAuthResponse(user);
        }

        @Transactional
        public void verifyEmail(String token) {
                User user = userRepository.findByVerificationToken(token)
                                .orElseThrow(() -> new RuntimeException("Invalid or expired verification token"));

                user.setEmailVerified(true);
                user.setVerificationToken(null);
                userRepository.save(user);
                log.info("Email verified for: {}", user.getEmail());
        }

        @Transactional
        public AuthResponse refreshToken(TokenRefreshRequest request) {
                RefreshToken refreshToken = refreshTokenService
                                .findByToken(request.getRefreshToken())
                                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

                refreshTokenService.verifyExpiration(refreshToken);

                User user = refreshToken.getUser();

                return generateAuthResponse(user);
        }

        @Transactional
        public void logout(String email) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                refreshTokenService.deleteByUser(user);
                log.info("User logged out: {}", email);
        }

        @Transactional
        public String forgotPassword(String email) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

                String resetToken = UUID.randomUUID().toString();
                user.setVerificationToken(resetToken);
                userRepository.save(user);

                if (featureFlags.isEmailVerification()) {
                        emailService.sendPasswordResetEmail(email, resetToken);
                        log.info("Password reset email sent to: {}", email);
                } else {
                        log.info("Password reset token for {}: {}", email, resetToken);
                }

                return resetToken;
        }

        @Transactional
        public void resetPassword(PasswordResetRequest request) {
                User user = userRepository.findByVerificationToken(request.getToken())
                                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

                user.setPassword(passwordEncoder.encode(request.getNewPassword()));
                user.setVerificationToken(null);
                userRepository.save(user);

                log.info("Password reset for: {}", user.getEmail());
        }

        private AuthResponse generateAuthResponse(User user) {
                UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                                .username(user.getEmail())
                                .password(user.getPassword() != null ? user.getPassword() : "")
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
                                                .twoFactorEnabled(user.isTwoFactorEnabled())
                                                .build())
                                .build();
        }
}
