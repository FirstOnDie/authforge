package com.authforge.security;

import com.authforge.model.AuthProvider;
import com.authforge.model.Role;
import com.authforge.model.User;
import com.authforge.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final UserRepository userRepository;
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // For testing
    public void setDelegate(OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = delegate.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = extractEmail(attributes);
        String name = extractName(attributes, registrationId);
        String providerId = extractProviderId(attributes);

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Email not available from " + registrationId);
        }

        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.setName(name);
            user.setProvider(provider);
            user.setProviderId(providerId);
            userRepository.save(user);
            log.info("OAuth2 user updated: {} ({})", email, provider);
        } else {
            User newUser = User.builder()
                    .email(email)
                    .name(name)
                    .provider(provider)
                    .providerId(providerId)
                    .role(Role.USER)
                    .enabled(true)
                    .build();
            userRepository.save(newUser);
            log.info("OAuth2 user created: {} ({})", email, provider);
        }

        return oAuth2User;
    }

    private String extractEmail(Map<String, Object> attributes) {
        return (String) attributes.get("email");
    }

    private String extractName(Map<String, Object> attributes, String registrationId) {
        if ("github".equals(registrationId)) {
            String name = (String) attributes.get("name");
            return name != null ? name : (String) attributes.get("login");
        }
        return (String) attributes.get("name");
    }

    private String extractProviderId(Map<String, Object> attributes) {
        Object id = attributes.get("sub");
        if (id == null) {
            id = attributes.get("id");
        }
        return id != null ? id.toString() : null;
    }
}
