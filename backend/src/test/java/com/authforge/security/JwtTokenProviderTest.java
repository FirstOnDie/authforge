package com.authforge.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        org.springframework.test.util.ReflectionTestUtils.setField(
                jwtTokenProvider, "jwtSecret",
                "test-secret-key-long-enough-for-hs256-algorithm-to-work-properly-1234");
        org.springframework.test.util.ReflectionTestUtils.setField(
                jwtTokenProvider, "accessTokenExpiration", 900000L);
    }

    @Test
    void shouldGenerateAndValidateToken() {
        UserDetails userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .roles("USER")
                .build();

        String token = jwtTokenProvider.generateAccessToken(userDetails);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.extractUsername(token)).isEqualTo("test@example.com");
    }

    @Test
    void shouldRejectInvalidToken() {
        assertThat(jwtTokenProvider.validateToken("invalid.token.here")).isFalse();
    }

    @Test
    void shouldReturnCorrectExpiration() {
        assertThat(jwtTokenProvider.getAccessTokenExpiration()).isEqualTo(900000L);
    }
}
