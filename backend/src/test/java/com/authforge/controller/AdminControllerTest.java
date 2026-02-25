package com.authforge.controller;

import com.authforge.config.FeatureFlags;
import com.authforge.model.Role;
import com.authforge.model.User;
import com.authforge.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminController.class, excludeAutoConfiguration = {
        OAuth2ClientAutoConfiguration.class }, properties = {
                "authforge.cors.allowed-origins=http://localhost:3000" })
@org.springframework.test.context.ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    private User testUser1;
    private User testUser2;

    @BeforeEach
    void setUp() {
        testUser1 = User.builder()
                .id(1L)
                .email("admin@example.com")
                .name("Admin User")
                .role(Role.ADMIN)
                .build();

        testUser2 = User.builder()
                .id(2L)
                .email("user@example.com")
                .name("Regular User")
                .role(Role.USER)
                .build();
    }

    @Test
    void shouldGetAllUsers() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(testUser1, testUser2));

        mockMvc.perform(get("/api/admin/users")
                .principal(new UsernamePasswordAuthenticationToken("admin@example.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].email").value("admin@example.com"))
                .andExpect(jsonPath("$[0].role").value("ADMIN"))
                .andExpect(jsonPath("$[1].email").value("user@example.com"))
                .andExpect(jsonPath("$[1].role").value("USER"));
    }

    @Test
    void shouldChangeUserRole() throws Exception {
        User updatedUser = User.builder()
                .id(2L)
                .email("user@example.com")
                .name("Regular User")
                .role(Role.ADMIN)
                .build();

        when(userService.changeRole(2L, "ADMIN")).thenReturn(updatedUser);

        Map<String, String> body = Map.of("role", "ADMIN");

        mockMvc.perform(put("/api/admin/users/2/role")
                .principal(new UsernamePasswordAuthenticationToken("admin@example.com", null))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void shouldGetFeatureFlags() throws Exception {
        when(featureFlags.isOauth2()).thenReturn(true);
        when(featureFlags.isTwoFactor()).thenReturn(false);
        when(featureFlags.isRateLimiting()).thenReturn(true);
        when(featureFlags.isEmailVerification()).thenReturn(false);

        mockMvc.perform(get("/api/admin/features")
                .principal(new UsernamePasswordAuthenticationToken("admin@example.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oauth2").value(true))
                .andExpect(jsonPath("$.twoFactor").value(false))
                .andExpect(jsonPath("$.rateLimiting").value(true))
                .andExpect(jsonPath("$.emailVerification").value(false));
    }
}
