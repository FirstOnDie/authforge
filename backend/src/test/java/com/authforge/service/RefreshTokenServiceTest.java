package com.authforge.service;

import com.authforge.model.RefreshToken;
import com.authforge.model.Role;
import com.authforge.model.User;
import com.authforge.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;

    private User testUser;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository);
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpiration", 604800000L);

        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .role(Role.USER)
                .build();
    }

    @Test
    void shouldCreateRefreshToken() {
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        RefreshToken token = refreshTokenService.createRefreshToken(testUser);

        assertThat(token.getUser()).isEqualTo(testUser);
        assertThat(token.getToken()).isNotBlank();
        assertThat(token.getExpiryDate()).isAfter(Instant.now());
    }

    @Test
    void shouldFindByToken() {
        RefreshToken refreshToken = RefreshToken.builder()
                .token("test-token")
                .user(testUser)
                .expiryDate(Instant.now().plusMillis(604800000))
                .build();

        when(refreshTokenRepository.findByToken("test-token")).thenReturn(Optional.of(refreshToken));

        Optional<RefreshToken> result = refreshTokenService.findByToken("test-token");
        assertThat(result).isPresent();
        assertThat(result.get().getToken()).isEqualTo("test-token");
    }

    @Test
    void shouldVerifyValidToken() {
        RefreshToken refreshToken = RefreshToken.builder()
                .token("valid-token")
                .user(testUser)
                .expiryDate(Instant.now().plusMillis(604800000))
                .build();

        RefreshToken result = refreshTokenService.verifyExpiration(refreshToken);
        assertThat(result).isNotNull();
    }

    @Test
    void shouldRejectExpiredToken() {
        RefreshToken refreshToken = RefreshToken.builder()
                .token("expired-token")
                .user(testUser)
                .expiryDate(Instant.now().minusMillis(1000))
                .build();

        assertThatThrownBy(() -> refreshTokenService.verifyExpiration(refreshToken))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void shouldDeleteByUser() {
        refreshTokenService.deleteByUser(testUser);
        verify(refreshTokenRepository).deleteByUser(testUser);
    }
}
