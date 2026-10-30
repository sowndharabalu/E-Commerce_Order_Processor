package model;

import java.time.LocalDateTime;

import model.Enums.Category;

public record Product(Integer id, String name, String description, double price, int stock, Category category,
                      LocalDateTime createdAt) {
    public Product {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be null");
        }
        if (price <= 0) {
            throw new IllegalArgumentException("Product price cannot be negative or zero");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("Product stock cannot be negative");
        }
    }

    public static Product createProduct(String name, String description, double price, int stock, Category category, LocalDateTime createdAt) {
        return new Product(null, name, description, price, stock, category, createdAt);
    }
}
