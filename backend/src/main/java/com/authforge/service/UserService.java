package com.authforge.service;

import com.authforge.model.Role;
import com.authforge.model.User;
import com.authforge.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private static final String USER_NOT_FOUND_ID = "User not found with id: ";

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new com.authforge.exception.ResourceNotFoundException("User not found: " + email));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User changeRole(Long userId, String newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.authforge.exception.ResourceNotFoundException(USER_NOT_FOUND_ID + userId));

        user.setRole(Role.valueOf(newRole.toUpperCase()));
        return userRepository.save(user);
    }

    @Transactional
    public void enableTwoFactor(Long userId, String secret) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.authforge.exception.ResourceNotFoundException(USER_NOT_FOUND_ID + userId));

        user.setTwoFactorSecret(secret);
        user.setTwoFactorEnabled(true);
        userRepository.save(user);
    }

    @Transactional
    public void disableTwoFactor(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.authforge.exception.ResourceNotFoundException(USER_NOT_FOUND_ID + userId));

        user.setTwoFactorSecret(null);
        user.setTwoFactorEnabled(false);
        userRepository.save(user);
    }
}
