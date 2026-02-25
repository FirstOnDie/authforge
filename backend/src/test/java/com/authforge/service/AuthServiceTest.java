package com.authforge.service;

import com.authforge.config.FeatureFlags;
import com.authforge.dto.AuthResponse;
import com.authforge.dto.LoginRequest;
import com.authforge.dto.RegisterRequest;
import com.authforge.dto.TokenRefreshRequest;
import com.authforge.model.RefreshToken;
import com.authforge.model.Role;
import com.authforge.model.User;
import com.authforge.repository.UserRepository;
import com.authforge.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private TotpService totpService;
    @Mock
    private EmailService emailService;

    private FeatureFlags featureFlags;
    private AuthService authService;

    private User testUser;
    private RefreshToken testRefreshToken;

    @BeforeEach
    void setUp() {
        featureFlags = new FeatureFlags();
        featureFlags.setEmailVerification(false);
        featureFlags.setTwoFactor(true);

        authService = new AuthService(
                userRepository, passwordEncoder, jwtTokenProvider,
                refreshTokenService, authenticationManager,
                totpService, emailService, featureFlags);

        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .password("encoded-password")
                .role(Role.USER)
                .emailVerified(true)
                .build();

        testRefreshToken = RefreshToken.builder()
                .id(1L)
                .token("refresh-token-value")
                .user(testUser)
                .expiryDate(Instant.now().plusMillis(604800000))
                .build();
    }

    @Test
    void shouldRegisterUser() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(900000L);
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(testRefreshToken);

        AuthResponse response = authService.register(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldFailRegisterDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    void shouldLoginUser() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(900000L);
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(testRefreshToken);

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.isRequiresTwoFactor()).isFalse();
    }

    @Test
    void shouldRequire2faWhenEnabled() {
        testUser.setTwoFactorEnabled(true);
        testUser.setTwoFactorSecret("secret");

        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        AuthResponse response = authService.login(request);

        assertThat(response.isRequiresTwoFactor()).isTrue();
        assertThat(response.getAccessToken()).isNull();
    }

    @Test
    void shouldVerifyTwoFactor() {
        testUser.setTwoFactorEnabled(true);
        testUser.setTwoFactorSecret("secret");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(totpService.verifyCode("secret", "123456")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(900000L);
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(testRefreshToken);

        AuthResponse response = authService.verifyTwoFactor("test@example.com", "123456");

        assertThat(response.getAccessToken()).isEqualTo("access-token");
    }

    @Test
    void shouldFailInvalid2faCode() {
        testUser.setTwoFactorEnabled(true);
        testUser.setTwoFactorSecret("secret");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(totpService.verifyCode("secret", "000000")).thenReturn(false);

        assertThatThrownBy(() -> authService.verifyTwoFactor("test@example.com", "000000"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid 2FA code");
    }

    @Test
    void shouldLogout() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        authService.logout("test@example.com");

        verify(refreshTokenService).deleteByUser(testUser);
    }

    @Test
    void shouldVerifyEmail() {
        when(userRepository.findByVerificationToken("token123")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        authService.verifyEmail("token123");

        assertThat(testUser.isEmailVerified()).isTrue();
        assertThat(testUser.getVerificationToken()).isNull();
    }

    @Test
    void shouldSendVerificationEmailWhenFeatureEnabled() {
        featureFlags.setEmailVerification(true);

        RegisterRequest request = new RegisterRequest();
        request.setName("New User");
        request.setEmail("new@example.com");
        request.setPassword("password123");

        User savedUser = User.builder()
                .id(2L).email("new@example.com").name("New User")
                .password("encoded").role(Role.USER).emailVerified(false).build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        AuthResponse response = authService.register(request);

        assertThat(response.isRequiresEmailVerification()).isTrue();
        verify(emailService).sendVerificationEmail(eq("new@example.com"), anyString());
    }

    @Test
    void shouldFailLoginIfUserNotFound() {
        LoginRequest request = new LoginRequest();
        request.setEmail("unknown@example.com");
        request.setPassword("password123");

        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(com.authforge.exception.ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void shouldFailLoginWhenEmailNotVerifiedAndFeatureEnabled() {
        featureFlags.setEmailVerification(true);
        testUser.setEmailVerified(false);

        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(com.authforge.exception.BadRequestException.class)
                .hasMessageContaining("Please verify your email before logging in");
    }

    @Test
    void shouldFailVerifyTwoFactorIfTwoFactorDisabledGlobally() {
        featureFlags.setTwoFactor(false);
        assertThatThrownBy(() -> authService.verifyTwoFactor("test@example.com", "123456"))
                .isInstanceOf(com.authforge.exception.BadRequestException.class)
                .hasMessageContaining("Two-factor authentication is disabled");
    }

    @Test
    void shouldFailVerifyTwoFactorIfUserTwoFactorDisabled() {
        testUser.setTwoFactorEnabled(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.verifyTwoFactor("test@example.com", "123456"))
                .isInstanceOf(com.authforge.exception.BadRequestException.class)
                .hasMessageContaining("Two-factor authentication is not enabled");
    }

    @Test
    void shouldFailVerifyEmailWithInvalidToken() {
        when(userRepository.findByVerificationToken("invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyEmail("invalid"))
                .isInstanceOf(com.authforge.exception.BadRequestException.class)
                .hasMessageContaining("Invalid or expired verification token");
    }

    @Test
    void shouldFailRefreshTokenIfTokenNotFound() {
        com.authforge.dto.TokenRefreshRequest req = new com.authforge.dto.TokenRefreshRequest();
        req.setRefreshToken("invalid");

        when(refreshTokenService.findByToken("invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken(req))
                .isInstanceOf(com.authforge.exception.ResourceNotFoundException.class)
                .hasMessageContaining("Refresh token not found");
    }

    @Test
    void shouldFailLogoutIfUserNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.logout("unknown@example.com"))
                .isInstanceOf(com.authforge.exception.ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void shouldForgotPassword() {
        featureFlags.setEmailVerification(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        String token = authService.forgotPassword("test@example.com");

        assertThat(token).isNotNull();
        verify(userRepository).save(testUser);
        verify(emailService).sendPasswordResetEmail(eq("test@example.com"), anyString());
    }

    @Test
    void shouldForgotPasswordWithoutEmailVerificationFeature() {
        featureFlags.setEmailVerification(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        String token = authService.forgotPassword("test@example.com");

        assertThat(token).isNotNull();
        verify(userRepository).save(testUser);
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void shouldFailForgotPasswordIfUserNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.forgotPassword("unknown@example.com"))
                .isInstanceOf(com.authforge.exception.ResourceNotFoundException.class)
                .hasMessageContaining("User not found with email: unknown@example.com");
    }

    @Test
    void shouldResetPassword() {
        com.authforge.dto.PasswordResetRequest request = new com.authforge.dto.PasswordResetRequest();
        request.setToken("reset123");
        request.setNewPassword("newpass123");

        when(userRepository.findByVerificationToken("reset123")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newpass123")).thenReturn("encoded-newpass123");

        authService.resetPassword(request);

        assertThat(testUser.getPassword()).isEqualTo("encoded-newpass123");
        assertThat(testUser.getVerificationToken()).isNull();
        verify(userRepository).save(testUser);
    }

    @Test
    void shouldFailResetPasswordIfTokenInvalid() {
        com.authforge.dto.PasswordResetRequest request = new com.authforge.dto.PasswordResetRequest();
        request.setToken("invalid");

        when(userRepository.findByVerificationToken("invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(com.authforge.exception.BadRequestException.class)
                .hasMessageContaining("Invalid or expired reset token");
    }

    @Test
    void shouldLoginUserWithNullPassword() {
        testUser.setPassword(null);

        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(900000L);
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(testRefreshToken);

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.isRequiresTwoFactor()).isFalse();
    }

    @Test
    void shouldFailVerifyTwoFactorIfSecretIsNull() {
        testUser.setTwoFactorEnabled(true);
        testUser.setTwoFactorSecret(null);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.verifyTwoFactor("test@example.com", "123456"))
                .isInstanceOf(com.authforge.exception.BadRequestException.class)
                .hasMessageContaining("Two-factor authentication is not enabled");
    }

    @Test
    void shouldLoginUserWhenEmailVerifiedAndFeatureEnabled() {
        featureFlags.setEmailVerification(true);
        testUser.setEmailVerified(true);

        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(900000L);
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(testRefreshToken);

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.isRequiresTwoFactor()).isFalse();
    }

    @Test
    void shouldLoginUserWhenTwoFactorDisabledGlobally() {
        featureFlags.setTwoFactor(false);
        testUser.setTwoFactorEnabled(true);

        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(900000L);
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(testRefreshToken);

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.isRequiresTwoFactor()).isFalse();
    }

    @Test
    void shouldRefreshTokenSuccessfully() {
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken("refresh-token-value");

        when(refreshTokenService.findByToken("refresh-token-value")).thenReturn(Optional.of(testRefreshToken));
        when(refreshTokenService.verifyExpiration(testRefreshToken)).thenReturn(testRefreshToken);
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("new-access-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(900000L);
        when(refreshTokenService.createRefreshToken(testUser)).thenReturn(testRefreshToken);

        AuthResponse response = authService.refreshToken(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token-value");
    }
}
