package com.authforge.service;

import com.authforge.config.FeatureFlags;
import com.authforge.dto.AuthResponse;
import com.authforge.dto.LoginRequest;
import com.authforge.dto.RegisterRequest;
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
}
