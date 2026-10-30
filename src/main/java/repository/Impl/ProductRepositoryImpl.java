package repository.Impl;

import model.Product;
import model.Enums.Category;
import repository.JdbcSupport;
import repository.ProductRepository;
import util.DataAccessException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ProductRepositoryImpl implements ProductRepository {
    private static final String SELECT =
            "SELECT product_id, name, description, price, stock, category, created_at FROM products ";

    private static Product map(ResultSet rs) throws SQLException {
        String cat = rs.getString("category");
        return new Product(
                rs.getInt("product_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getDouble("price"),
                rs.getInt("stock"),
                cat == null ? null : Category.valueOf(cat),
                JdbcSupport.toLocalDateTime(rs.getTimestamp("created_at")));
    }

    @Override
    public Product save(Product p) {
        if (p.id() == null) {
            int id = JdbcSupport.insert(
                    "INSERT INTO products (name, description, price, stock, category) VALUES (?, ?, ?, ?, ?)",
                    ps -> {
                        ps.setString(1, p.name());
                        ps.setString(2, p.description());
                        ps.setDouble(3, p.price());
                        ps.setInt(4, p.stock());
                        ps.setString(5, p.category() == null ? null : p.category().name());
                    });
            return findById(id).orElseThrow(() -> new DataAccessException("Product vanished after insert"));
        }
        JdbcSupport.update(
                "UPDATE products SET name = ?, description = ?, price = ?, stock = ?, category = ? WHERE product_id = ?",
                ps -> {
                    ps.setString(1, p.name());
                    ps.setString(2, p.description());
                    ps.setDouble(3, p.price());
                    ps.setInt(4, p.stock());
                    ps.setString(5, p.category() == null ? null : p.category().name());
                    ps.setInt(6, p.id());
                });
        return findById(p.id()).orElse(p);
    }

    @Override
    public Optional<Product> findById(int id) {
        return JdbcSupport.queryOne(SELECT + "WHERE product_id = ?", ps -> ps.setInt(1, id), ProductRepositoryImpl::map);
    }

    @Override
    public Optional<Product> findById(int id, Connection conn) {
        return JdbcSupport.queryOne(conn, SELECT + "WHERE product_id = ?", ps -> ps.setInt(1, id), ProductRepositoryImpl::map);
    }

    @Override
    public List<Product> findAll() {
        return JdbcSupport.query(SELECT + "ORDER BY product_id", JdbcSupport.NO_PARAMS, ProductRepositoryImpl::map);
    }

    @Override
    public List<Product> findByCategory(Category category) {
        return JdbcSupport.query(SELECT + "WHERE category = ? ORDER BY product_id",
                ps -> ps.setString(1, category.name()), ProductRepositoryImpl::map);
    }

    @Override
    public boolean updateStock(int id, int newStock) {
        return JdbcSupport.update("UPDATE products SET stock = ? WHERE product_id = ?", ps -> {
            ps.setInt(1, newStock);
            ps.setInt(2, id);
        }) > 0;
    }

    @Override
    public boolean decreaseStock(int id, int quantity, Connection conn) {
        return JdbcSupport.update(conn,
                "UPDATE products SET stock = stock - ? WHERE product_id = ? AND stock >= ?", ps -> {
                    ps.setInt(1, quantity);
                    ps.setInt(2, id);
                    ps.setInt(3, quantity);
                }) > 0;
    }

    @Override
    public boolean increaseStock(int id, int quantity, Connection conn) {
        return JdbcSupport.update(conn,
                "UPDATE products SET stock = stock + ? WHERE product_id = ?", ps -> {
                    ps.setInt(1, quantity);
                    ps.setInt(2, id);
                }) > 0;
    }

    @Override
    public boolean delete(int id) {
        return JdbcSupport.update("DELETE FROM products WHERE product_id = ?", ps -> ps.setInt(1, id)) > 0;
    }
}
