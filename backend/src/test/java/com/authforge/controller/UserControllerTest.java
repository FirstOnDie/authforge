package com.authforge.controller;

import com.authforge.model.Role;
import com.authforge.model.User;
import com.authforge.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class, excludeAutoConfiguration = {
        OAuth2ClientAutoConfiguration.class }, properties = { "authforge.cors.allowed-origins=http://localhost:3000" })
@org.springframework.test.context.ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

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
                .role(Role.USER)
                .build();
    }

    @Test
    void shouldGetCurrentUser() throws Exception {
        when(userService.getUserByEmail("user@example.com")).thenReturn(testUser);

        mockMvc.perform(get("/api/users/me")
                .principal(new UsernamePasswordAuthenticationToken("user@example.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.role").value("USER"));
    }
}
