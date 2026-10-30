package service;

import model.User;
import repository.UserRepository;
import util.ApiException;
import util.PasswordHasher;
import util.Validator;

public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(String username, String email, String password) {
        if (!Validator.isValidUsername(username))
            throw ApiException.badRequest("Username must be 4-20 characters: letters, digits or underscore");
        if (!Validator.isValidEmail(email))
            throw ApiException.badRequest("Invalid email format");
        if (!Validator.isValidPassword(password))
            throw ApiException.badRequest("Password must be at least 8 characters");

        if (userRepository.findByUsername(username).isPresent())
            throw ApiException.conflict("Username already taken");
        if (userRepository.findByEmail(email).isPresent())
            throw ApiException.conflict("Email already registered");

        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hashPass(password, salt);
        return userRepository.save(new User(null, username, email, hash, salt, null));
    }

    public User login(String username, String password) {
        if (username == null || password == null)
            throw ApiException.badRequest("Username and password are required");
        return userRepository.findByUsername(username)
                .filter(u -> PasswordHasher.verifyPass(password, u.salt(), u.passwordHash()))
                .orElseThrow(() -> ApiException.unauthorized("Invalid username or password"));
    }

    public User getUser(int id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("User not found: " + id));
    }
}