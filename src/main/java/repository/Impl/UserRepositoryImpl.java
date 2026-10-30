package repository.Impl;

import model.User;
import repository.JdbcSupport;
import repository.UserRepository;
import util.DataAccessException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UserRepositoryImpl implements UserRepository {
    private static final String SELECT =
            "SELECT user_id, username, email, password_hash, salt, created_at FROM users ";

    private static User map(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("salt"),
                JdbcSupport.toLocalDateTime(rs.getTimestamp("created_at")));
    }

    @Override
    public User save(User u) {
        if (u.id() == null) {
            int id = JdbcSupport.insert(
                    "INSERT INTO users (username, email, password_hash, salt) VALUES (?, ?, ?, ?)",
                    ps -> {
                        ps.setString(1, u.username());
                        ps.setString(2, u.email());
                        ps.setString(3, u.passwordHash());
                        ps.setString(4, u.salt());
                    });
            return findById(id).orElseThrow(() -> new DataAccessException("User vanished after insert"));
        }
        JdbcSupport.update(
                "UPDATE users SET username = ?, email = ?, password_hash = ?, salt = ? WHERE user_id = ?",
                ps -> {
                    ps.setString(1, u.username());
                    ps.setString(2, u.email());
                    ps.setString(3, u.passwordHash());
                    ps.setString(4, u.salt());
                    ps.setInt(5, u.id());
                });
        return findById(u.id()).orElse(u);
    }

    @Override
    public Optional<User> findById(int id) {
        return JdbcSupport.queryOne(SELECT + "WHERE user_id = ?", ps -> ps.setInt(1, id), UserRepositoryImpl::map);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return JdbcSupport.queryOne(SELECT + "WHERE username = ?", ps -> ps.setString(1, username), UserRepositoryImpl::map);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return JdbcSupport.queryOne(SELECT + "WHERE email = ?", ps -> ps.setString(1, email), UserRepositoryImpl::map);
    }

    @Override
    public boolean delete(int id) {
        return JdbcSupport.update("DELETE FROM users WHERE user_id = ?", ps -> ps.setInt(1, id)) > 0;
    }
}