package com.authforge.controller;

import com.authforge.config.FeatureFlags;
import com.authforge.model.User;
import com.authforge.service.TotpService;
import com.authforge.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(controllers = TwoFactorController.class, excludeAutoConfiguration = {
        OAuth2ClientAutoConfiguration.class }, properties = { "authforge.cors.allowed-origins=http://localhost:3000" })
@org.springframework.test.context.ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class TwoFactorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TotpService totpService;

    @MockBean
    private UserService userService;

    @MockBean
    private FeatureFlags featureFlags;

    // Security beans required for context
    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;
    @MockBean
    private com.authforge.security.CustomOAuth2UserService customOAuth2UserService;
    @MockBean
    private com.authforge.security.OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    @MockBean
    private com.authforge.security.JwtAuthFilter jwtAuthFilter;
    @MockBean
    private com.authforge.security.RateLimitFilter rateLimitFilter;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .name("Test User")
                .build();
    }

    @Test
    void shouldSetupTwoFactor() throws Exception {
        when(featureFlags.isTwoFactor()).thenReturn(true);
        when(userService.getUserByEmail("user@example.com")).thenReturn(testUser);
        when(totpService.generateSecret()).thenReturn("SECRET123");
        when(totpService.generateQrUri("SECRET123", "user@example.com")).thenReturn("otpauth://totp/AuthForge:user@example.com?secret=SECRET123&issuer=AuthForge");

        mockMvc.perform(post("/api/2fa/setup")
                .principal(new UsernamePasswordAuthenticationToken("user@example.com", null))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secret").value("SECRET123"))
                .andExpect(jsonPath("$.qrUri").value("otpauth://totp/AuthForge:user@example.com?secret=SECRET123&issuer=AuthForge"));
    }

    @Test
    void shouldFailSetupIfTwoFactorDisabled() throws Exception {
        when(featureFlags.isTwoFactor()).thenReturn(false);

        mockMvc.perform(post("/api/2fa/setup")
                .principal(new UsernamePasswordAuthenticationToken("user@example.com", null))
                .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Two-factor authentication is disabled"));
    }

    @Test
    void shouldEnableTwoFactor() throws Exception {
        when(featureFlags.isTwoFactor()).thenReturn(true);
        when(userService.getUserByEmail("user@example.com")).thenReturn(testUser);
        when(totpService.verifyCode("SECRET123", "123456")).thenReturn(true);

        Map<String, String> body = Map.of("secret", "SECRET123", "code", "123456");

        mockMvc.perform(post("/api/2fa/enable")
                .principal(new UsernamePasswordAuthenticationToken("user@example.com", null))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Two-factor authentication enabled"));

        verify(userService, times(1)).enableTwoFactor(1L, "SECRET123");
    }

    @Test
    void shouldFailToEnableIfInvalidCode() throws Exception {
        when(featureFlags.isTwoFactor()).thenReturn(true);
        when(userService.getUserByEmail("user@example.com")).thenReturn(testUser);
        when(totpService.verifyCode("SECRET123", "wrong")).thenReturn(false);

        Map<String, String> body = Map.of("secret", "SECRET123", "code", "wrong");

        mockMvc.perform(post("/api/2fa/enable")
                .principal(new UsernamePasswordAuthenticationToken("user@example.com", null))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDisableTwoFactor() throws Exception {
        when(featureFlags.isTwoFactor()).thenReturn(true);
        when(userService.getUserByEmail("user@example.com")).thenReturn(testUser);

        mockMvc.perform(post("/api/2fa/disable")
                .principal(new UsernamePasswordAuthenticationToken("user@example.com", null))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Two-factor authentication disabled"));

        verify(userService, times(1)).disableTwoFactor(1L);
    }

    @Test
    void shouldFailToEnableIfTwoFactorDisabled() throws Exception {
        when(featureFlags.isTwoFactor()).thenReturn(false);

        Map<String, String> body = Map.of("secret", "SECRET123", "code", "123456");

        mockMvc.perform(post("/api/2fa/enable")
                .principal(new UsernamePasswordAuthenticationToken("user@example.com", null))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Two-factor authentication is disabled"));
    }

    @Test
    void shouldFailToDisableIfTwoFactorDisabled() throws Exception {
        when(featureFlags.isTwoFactor()).thenReturn(false);

        mockMvc.perform(post("/api/2fa/disable")
                .principal(new UsernamePasswordAuthenticationToken("user@example.com", null))
                .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Two-factor authentication is disabled"));
    }
}
