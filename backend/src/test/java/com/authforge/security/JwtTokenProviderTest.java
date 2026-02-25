package com.authforge.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        String secret = "test-secret-key-long-enough-for-hs256-algorithm-to-work-properly-1234";
        jwtTokenProvider = new JwtTokenProvider(secret, 900000L);

        userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .roles("USER")
                .build();
    }

    @Test
    void shouldGenerateToken() {
        String token = jwtTokenProvider.generateAccessToken(userDetails);
        assertThat(token).isNotBlank();
    }

    @Test
    void shouldExtractEmail() {
        String token = jwtTokenProvider.generateAccessToken(userDetails);
        assertThat(jwtTokenProvider.extractEmail(token)).isEqualTo("test@example.com");
    }

    @Test
    void shouldValidateToken() {
        String token = jwtTokenProvider.generateAccessToken(userDetails);
        assertThat(jwtTokenProvider.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void shouldRejectInvalidToken() {
        assertThat(jwtTokenProvider.isTokenValid("invalid.token.here", userDetails)).isFalse();
    }

    @Test
    void shouldReturnCorrectExpiration() {
        assertThat(jwtTokenProvider.getAccessTokenExpiration()).isEqualTo(900000L);
    }

    @Test
    void shouldRejectTokenWithDifferentEmail() {
        String token = jwtTokenProvider.generateAccessToken(userDetails);
        UserDetails differentUser = User.builder()
                .username("wrong@example.com")
                .password("password")
                .roles("USER")
                .build();
        assertThat(jwtTokenProvider.isTokenValid(token, differentUser)).isFalse();
    }
}
