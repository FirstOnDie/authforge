package com.authforge.security;

import com.authforge.model.RefreshToken;
import com.authforge.model.Role;
import com.authforge.model.User;
import com.authforge.repository.UserRepository;
import com.authforge.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private Authentication authentication;
    @Mock
    private OAuth2User oAuth2User;

    private OAuth2AuthenticationSuccessHandler successHandler;

    @BeforeEach
    void setUp() {
        successHandler = new OAuth2AuthenticationSuccessHandler(
                jwtTokenProvider, refreshTokenService, userRepository, "http://localhost:3000/oauth-callback");
    }

    @Test
    void onAuthenticationSuccess_withEmail_redirectsWithTokens() throws Exception {
        User user = User.builder().id(1L).email("test@example.com").password("").name("test").role(Role.USER).build();
        RefreshToken rt = new RefreshToken();
        rt.setToken("refresh-token");

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email")).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(rt);
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600000L);

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect(contains("token=access-token"));
        verify(response).sendRedirect(contains("refreshToken=refresh-token"));
        verify(response).sendRedirect(contains("expiresIn=3600000"));
    }

    @Test
    void onAuthenticationSuccess_withLoginOnly_redirectsWithTokens() throws Exception {
        User user = User.builder().id(1L).email("githubuser@github.com").password("pwd").name("git").role(Role.USER)
                .build();
        RefreshToken rt = new RefreshToken();
        rt.setToken("refresh-token-git");

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email")).thenReturn(null);
        when(oAuth2User.getAttribute("login")).thenReturn("githubuser");
        when(userRepository.findByEmail("githubuser@github.com")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("token-git");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(rt);
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600000L);

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect(contains("token=token-git"));
        verify(response).sendRedirect(contains("refreshToken=refresh-token-git"));
    }

    @Test
    void onAuthenticationSuccess_withNullPassword_redirectsWithTokens() throws Exception {
        User user = User.builder().id(1L).email("nullpwd@github.com").password(null).name("git").role(Role.USER)
                .build();
        RefreshToken rt = new RefreshToken();
        rt.setToken("refresh-token-git");

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email")).thenReturn("nullpwd@github.com");
        when(userRepository.findByEmail("nullpwd@github.com")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("token-git");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(rt);
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600000L);

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect(contains("token=token-git"));
        verify(response).sendRedirect(contains("refreshToken=refresh-token-git"));
    }
}
