package com.authforge.security;

import com.authforge.model.AuthProvider;
import com.authforge.model.User;
import com.authforge.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;

    private CustomOAuth2UserService customOAuth2UserService;

    @BeforeEach
    void setUp() {
        customOAuth2UserService = new CustomOAuth2UserService(userRepository);
        customOAuth2UserService.setDelegate(delegate);
    }

    private OAuth2UserRequest createMockRequest(String registrationId) {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId(registrationId)
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/{action}/oauth2/code/{registrationId}")
                .authorizationUri("https://example.com/oauth2/authorize")
                .tokenUri("https://example.com/oauth2/token")
                .userInfoUri("https://example.com/userinfo")
                .userNameAttributeName("id")
                .clientName("Client Name")
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "token", Instant.now(), Instant.now().plusSeconds(3600));

        return new OAuth2UserRequest(clientRegistration, accessToken);
    }

    @Test
    void loadUser_NewGoogleUser_CreatesAndSavesUser() {
        OAuth2UserRequest request = createMockRequest("google");
        OAuth2User mockOAuth2User = new DefaultOAuth2User(Collections.emptyList(),
                Map.of("email", "new@gmail.com", "name", "Google User", "sub", "12345"), "email");

        when(delegate.loadUser(request)).thenReturn(mockOAuth2User);
        when(userRepository.findByEmail("new@gmail.com")).thenReturn(Optional.empty());

        OAuth2User result = customOAuth2UserService.loadUser(request);

        assertEquals(mockOAuth2User, result);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("new@gmail.com", savedUser.getEmail());
        assertEquals("Google User", savedUser.getName());
        assertEquals(AuthProvider.GOOGLE, savedUser.getProvider());
        assertEquals("12345", savedUser.getProviderId());
    }

    @Test
    void loadUser_ExistingGithubUser_UpdatesAndSavesUser() {
        OAuth2UserRequest request = createMockRequest("github");
        // Github uses "login" if "name" is missing, and "id" instead of "sub"
        OAuth2User mockOAuth2User = new DefaultOAuth2User(Collections.emptyList(),
                Map.of("email", "existing@github.com", "login", "octocat", "id", 67890), "login");

        when(delegate.loadUser(request)).thenReturn(mockOAuth2User);

        User existingUser = User.builder().id(1L).email("existing@github.com").build();
        when(userRepository.findByEmail("existing@github.com")).thenReturn(Optional.of(existingUser));

        customOAuth2UserService.loadUser(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User updatedUser = userCaptor.getValue();
        assertEquals(1L, updatedUser.getId());
        assertEquals("octocat", updatedUser.getName());
        assertEquals(AuthProvider.GITHUB, updatedUser.getProvider());
        assertEquals("67890", updatedUser.getProviderId());
    }

    @Test
    void loadUser_GithubUserWithName_UsesName() {
        OAuth2UserRequest request = createMockRequest("github");
        OAuth2User mockOAuth2User = new DefaultOAuth2User(Collections.emptyList(),
                Map.of("email", "name@github.com", "name", "Octo Cat", "id", 111), "name");

        when(delegate.loadUser(request)).thenReturn(mockOAuth2User);
        when(userRepository.findByEmail("name@github.com")).thenReturn(Optional.empty());

        customOAuth2UserService.loadUser(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("Octo Cat", userCaptor.getValue().getName());
    }

    @Test
    void loadUser_MissingEmail_ThrowsException() {
        OAuth2UserRequest request = createMockRequest("google");
        OAuth2User mockOAuth2User = new DefaultOAuth2User(Collections.emptyList(),
                Map.of("name", "No Email"), "name");

        when(delegate.loadUser(request)).thenReturn(mockOAuth2User);

        assertThrows(OAuth2AuthenticationException.class, () -> customOAuth2UserService.loadUser(request));
    }

    @Test
    void loadUser_MissingSubAndId_UsesNullProviderId() {
        OAuth2UserRequest request = createMockRequest("google");
        OAuth2User mockOAuth2User = new DefaultOAuth2User(Collections.emptyList(),
                Map.of("email", "nomissingid@gmail.com", "name", "Google User"), "email");

        when(delegate.loadUser(request)).thenReturn(mockOAuth2User);
        when(userRepository.findByEmail("nomissingid@gmail.com")).thenReturn(Optional.empty());

        customOAuth2UserService.loadUser(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertNull(userCaptor.getValue().getProviderId());
    }

    @Test
    void loadUser_BlankEmail_ThrowsException() {
        OAuth2UserRequest request = createMockRequest("google");
        OAuth2User mockOAuth2User = new DefaultOAuth2User(Collections.emptyList(),
                Map.of("email", "   ", "name", "Blank Email"), "email");

        when(delegate.loadUser(request)).thenReturn(mockOAuth2User);

        assertThrows(OAuth2AuthenticationException.class, () -> customOAuth2UserService.loadUser(request));
    }
}
