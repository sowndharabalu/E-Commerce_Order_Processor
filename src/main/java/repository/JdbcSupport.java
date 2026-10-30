package repository;

import config.DatabaseConfig;
import util.ApiException;
import util.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public final class JdbcSupport {
    private JdbcSupport() {
    }

    @FunctionalInterface
    public interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    @FunctionalInterface
    public interface ConnFunction<R> {
        R apply(Connection c) throws SQLException;
    }

    public static final Binder NO_PARAMS = ps -> {
    };

    private static RuntimeException translate(SQLException e) {
        if (e instanceof SQLIntegrityConstraintViolationException)
            return ApiException.conflict("Record already exists or violates a constraint");
        return new DataAccessException(e);
    }

    public static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }

    public static <R> R withConnection(ConnFunction<R> fn) {
        try (Connection c = DatabaseConfig.getConnection()) {
            return fn.apply(c);
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    public static <R> R inTransaction(ConnFunction<R> fn) {
        try (Connection c = DatabaseConfig.getConnection()) {
            c.setAutoCommit(false);
            try {
                R result = fn.apply(c);
                c.commit();
                return result;
            } catch (RuntimeException | SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    public static <T> List<T> query(Connection c, String sql, Binder binder, RowMapper<T> mapper) {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> out = new ArrayList<>();
                while (rs.next()) out.add(mapper.map(rs));
                return out;
            }
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    public static <T> Optional<T> queryOne(Connection c, String sql, Binder binder, RowMapper<T> mapper) {
        return query(c, sql, binder, mapper).stream().findFirst();
    }

    public static int update(Connection c, String sql, Binder binder) {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    public static int insert(Connection c, String sql, Binder binder) {
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            binder.bind(ps);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new DataAccessException("Insert did not return a generated key");
            }
        } catch (SQLException e) {
            throw translate(e);
        }
    }


    public static <T> List<T> query(String sql, Binder binder, RowMapper<T> mapper) {
        return withConnection(c -> query(c, sql, binder, mapper));
    }

    public static <T> Optional<T> queryOne(String sql, Binder binder, RowMapper<T> mapper) {
        return withConnection(c -> queryOne(c, sql, binder, mapper));
    }

    public static int update(String sql, Binder binder) {
        return withConnection(c -> update(c, sql, binder));
    }

    public static int insert(String sql, Binder binder) {
        return withConnection(c -> insert(c, sql, binder));
    }
}