package config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {
    public static final String URL = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/ecommerce_db");
    public static final String USER = System.getenv().getOrDefault("DB_USER", "root");
    public static final String PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "welcome");

    private DatabaseConfig() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
