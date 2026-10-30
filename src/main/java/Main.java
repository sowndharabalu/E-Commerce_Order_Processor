import config.DatabaseConfig;
import controller.OrderController;
import controller.ProductController;
import controller.UserController;
import repository.OrderRepository;
import repository.ProductRepository;
import repository.UserRepository;
import repository.Impl.OrderRepositoryImpl;
import repository.Impl.ProductRepositoryImpl;
import repository.Impl.UserRepositoryImpl;
import server.ApiServer;
import service.OrderService;
import service.ProductService;
import service.UserService;

import java.sql.Connection;

public class Main {
    public static void main(String[] args) throws Exception {
        try (Connection ignored = DatabaseConfig.getConnection()) {
            System.out.println("Connected to database.");
        } catch (Exception e) {
            System.err.println("Cannot connect to database (check DatabaseConfig): " + e.getMessage());
            System.exit(1);
        }

        UserRepository userRepository = new UserRepositoryImpl();
        ProductRepository productRepository = new ProductRepositoryImpl();
        OrderRepository orderRepository = new OrderRepositoryImpl();

        UserService userService = new UserService(userRepository);
        ProductService productService = new ProductService(productRepository);
        OrderService orderService = new OrderService(orderRepository, productRepository, userRepository);

        UserController userController = new UserController(userService);
        ProductController productController = new ProductController(productService);
        OrderController orderController = new OrderController(orderService);

        new ApiServer(8080, userController, productController, orderController).start();
        System.out.println("Server running on http://localhost:8080");
    }
}