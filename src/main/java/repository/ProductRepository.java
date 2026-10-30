package repository;

import model.Product;
import model.Enums.Category;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Product save(Product product);

    Optional<Product> findById(int id);

    Optional<Product> findById(int id, Connection conn);

    List<Product> findAll();

    List<Product> findByCategory(Category category);

    boolean updateStock(int id, int newStock);

    boolean decreaseStock(int id, int quantity, Connection conn);

    boolean increaseStock(int id, int quantity, Connection conn);

    boolean delete(int id);
}