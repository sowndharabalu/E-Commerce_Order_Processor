package service;

import model.Product;
import model.Enums.Category;
import repository.ProductRepository;
import util.ApiException;

import java.util.List;

public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product addProduct(String name, String description, double price, int stock, Category category) {
        if (name == null || name.isBlank())
            throw ApiException.badRequest("Product name is required");
        if (price <= 0)
            throw ApiException.badRequest("Price must be greater than 0");
        if (stock < 0)
            throw ApiException.badRequest("Stock cannot be negative");
        if (category == null)
            throw ApiException.badRequest("Category is required");
        return productRepository.save(new Product(null, name, description, price, stock, category, null));
    }

    public Product getProduct(int id) {
        return productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Product not found: " + id));
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public List<Product> getByCategory(Category category) {
        return productRepository.findByCategory(category);
    }

    public Product updateStock(int id, int newStock) {
        if (newStock < 0) throw ApiException.badRequest("Stock must be 0 or more");
        getProduct(id); // 404 if missing
        productRepository.updateStock(id, newStock);
        return getProduct(id);
    }
}