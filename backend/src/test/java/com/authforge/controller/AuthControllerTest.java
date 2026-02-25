package com.authforge.controller;

import com.authforge.dto.*;
import com.authforge.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class }, properties = {
                                "authforge.cors.allowed-origins=http://localhost:3000",
                                "authforge.app.frontend-url=http://localhost:3000"
                })
@org.springframework.test.context.ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private AuthService authService;

        // Security mocks to prevent application context load failure
        @MockBean
        private com.authforge.security.JwtAuthFilter jwtAuthFilter;
        @MockBean
        private com.authforge.security.RateLimitFilter rateLimitFilter;
        @MockBean
        private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;
        @MockBean
        private com.authforge.security.CustomOAuth2UserService customOAuth2UserService;
        @MockBean
        private com.authforge.security.OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
        @MockBean
        private com.authforge.config.FeatureFlags featureFlags;

        @Test
        void shouldRegisterUser() throws Exception {
                RegisterRequest request = new RegisterRequest();
                request.setEmail("test@example.com");
                request.setName("Test User");
                request.setPassword("Password123!");

                AuthResponse response = AuthResponse.builder().accessToken("mock-token").build();

                when(authService.register(any(RegisterRequest.class))).thenReturn(response);

                mockMvc.perform(post("/api/auth/register")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.accessToken").value("mock-token"));
        }

        @Test
        void shouldLoginUser() throws Exception {
                LoginRequest request = new LoginRequest();
                request.setEmail("test@example.com");
                request.setPassword("Password123!");

                AuthResponse response = AuthResponse.builder().accessToken("mock-token").build();

                when(authService.login(any(LoginRequest.class))).thenReturn(response);

                mockMvc.perform(post("/api/auth/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").value("mock-token"));
        }

        @Test
        void shouldVerifyTwoFactor() throws Exception {
                TwoFactorLoginRequest request = new TwoFactorLoginRequest();
                request.setEmail("test@example.com");
                request.setCode("123456");

                AuthResponse response = AuthResponse.builder().accessToken("mock-token").build();

                when(authService.verifyTwoFactor("test@example.com", "123456")).thenReturn(response);

                mockMvc.perform(post("/api/auth/2fa/verify")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").value("mock-token"));
        }

        @Test
        void shouldVerifyEmail() throws Exception {
                doNothing().when(authService).verifyEmail("valid-token");

                mockMvc.perform(get("/api/auth/verify")
                                .param("token", "valid-token"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message")
                                                .value("Email verified successfully! You can now log in."));
        }

        @Test
        void shouldRefreshToken() throws Exception {
                TokenRefreshRequest request = new TokenRefreshRequest();
                request.setRefreshToken("mock-refresh-token");

                AuthResponse response = AuthResponse.builder().accessToken("new-mock-token").build();

                when(authService.refreshToken(any(TokenRefreshRequest.class))).thenReturn(response);

                mockMvc.perform(post("/api/auth/refresh")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").value("new-mock-token"));
        }

        @Test
        void shouldLogoutUser() throws Exception {
                doNothing().when(authService).logout("testuser");

                mockMvc.perform(post("/api/auth/logout")
                                .principal(new UsernamePasswordAuthenticationToken("testuser", null))
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Logged out successfully"));
        }

    @Test
    void shouldRequestPasswordReset() throws Exception {
        when(authService.forgotPassword("test@example.com")).thenReturn("reset-token");

        mockMvc.perform(post("/api/auth/forgot-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If the email exists, a reset link has been sent."));
    }

        @Test
        void shouldResetPassword() throws Exception {
                PasswordResetRequest request = new PasswordResetRequest();
                request.setToken("reset-token");
                request.setNewPassword("newPassword123!");

                doNothing().when(authService).resetPassword(any(PasswordResetRequest.class));

                mockMvc.perform(post("/api/auth/reset-password")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Password reset successfully"));
        }
}
