package com.authforge.service;

import com.authforge.model.Role;
import com.authforge.model.User;
import com.authforge.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .role(Role.USER)
                .build();
    }

    @Test
    void shouldGetUserByEmail() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        User result = userService.getUserByEmail("test@example.com");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findByEmail("notfound@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByEmail("notfound@test.com"))
                .isInstanceOf(com.authforge.exception.ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void shouldGetAllUsers() {
        User user2 = User.builder().id(2L).email("u2@test.com").name("User 2").role(Role.ADMIN).build();
        when(userRepository.findAll()).thenReturn(Arrays.asList(testUser, user2));

        List<User> users = userService.getAllUsers();
        assertThat(users).hasSize(2);
    }

    @Test
    void shouldChangeRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.changeRole(1L, "ADMIN");
        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void shouldEnableTwoFactor() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.enableTwoFactor(1L, "SECRET123");

        assertThat(testUser.isTwoFactorEnabled()).isTrue();
        assertThat(testUser.getTwoFactorSecret()).isEqualTo("SECRET123");
        verify(userRepository).save(testUser);
    }

    @Test
    void shouldDisableTwoFactor() {
        testUser.setTwoFactorEnabled(true);
        testUser.setTwoFactorSecret("SECRET123");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.disableTwoFactor(1L);

        assertThat(testUser.isTwoFactorEnabled()).isFalse();
        assertThat(testUser.getTwoFactorSecret()).isNull();
        verify(userRepository).save(testUser);
    }
}
