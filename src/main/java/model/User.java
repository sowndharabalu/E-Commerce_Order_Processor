package model;

import util.Validator;

import java.time.LocalDateTime;

public record User(Integer id, String username, String email, String passwordHash, String salt,
                   LocalDateTime createdAt) {
    public User {
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        } else if (!Validator.isValidUsername(username)) {
            throw new IllegalArgumentException("Invalid username format");
        }
        if (email == null) {
            throw new IllegalArgumentException("Email cannot be null");
        } else if (!Validator.isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash cannot be empty");
        }
        if (salt == null || salt.isBlank()) {
            throw new IllegalArgumentException("Salt cannot be empty");
        }
    }

    public static User createUser(String username, String email, String passwordHash, String salt) {
        return new User(null, username, email, passwordHash, salt, LocalDateTime.now());
    }
}
