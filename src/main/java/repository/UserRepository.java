package repository;

import model.User;

import java.util.Optional;

public interface UserRepository {
    User save(User user);

    Optional<User> findById(int id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean delete(int id);
}
