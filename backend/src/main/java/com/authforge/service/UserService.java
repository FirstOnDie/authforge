package com.authforge.service;

import com.authforge.model.Role;
import com.authforge.model.User;
import com.authforge.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * User Service — handles user profile and admin operations.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get a user by their email address.
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    /**
     * Get all users (admin only).
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Change a user's role (admin only).
     */
    @Transactional
    public User changeRole(Long userId, String newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        user.setRole(Role.valueOf(newRole.toUpperCase()));
        return userRepository.save(user);
    }
}
