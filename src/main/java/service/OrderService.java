package service;

import model.Order;
import model.OrderItem;
import model.Product;
import model.Enums.OrderStatus;
import repository.JdbcSupport;
import repository.OrderRepository;
import repository.ProductRepository;
import repository.UserRepository;
import util.ApiException;
import util.DataAccessException;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository,
                        UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    public Order createOrder(int userId, Map<Integer, Integer> productQuantities) {
        if (productQuantities == null || productQuantities.isEmpty())
            throw ApiException.badRequest("Order must contain at least one item");
        if (productQuantities.values().stream().anyMatch(q -> q == null || q <= 0))
            throw ApiException.badRequest("Quantities must be greater than 0");

        userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found: " + userId));

        Map<Integer, Integer> sorted = new TreeMap<>(productQuantities);

        return JdbcSupport.inTransaction(conn -> {
            List<OrderItem> items = sorted.entrySet().stream().map(e -> {
                Product p = productRepository.findById(e.getKey(), conn)
                        .orElseThrow(() -> ApiException.notFound("Product not found: " + e.getKey()));
                if (p.stock() < e.getValue())
                    throw ApiException.badRequest("Insufficient stock for '" + p.name() + "': requested "
                            + e.getValue() + ", available " + p.stock());
                return new OrderItem(null, null, p.id(), e.getValue(), p.price());
            }).collect(Collectors.toList());

            double total = Math.round(items.stream().mapToDouble(OrderItem::getTotalPrice).sum() * 100.0) / 100.0;
            Order saved = orderRepository.save(new Order(null, userId, total, OrderStatus.PENDING, null, items), conn);

            items.forEach(i -> {
                if (!productRepository.decreaseStock(i.productId(), i.quantity(), conn))
                    throw ApiException.badRequest("Insufficient stock for product " + i.productId());
            });
            return saved;
        });
    }

    public Order confirmOrder(int orderId) {
        return JdbcSupport.inTransaction(conn -> {
            Order order = orderRepository.findById(orderId, conn)
                    .orElseThrow(() -> ApiException.notFound("Order not found: " + orderId));
            requirePending(order, "confirmed");
            orderRepository.updateStatus(orderId, OrderStatus.CONFIRMED, conn);
            return orderRepository.findById(orderId, conn)
                    .orElseThrow(() -> new DataAccessException("Order vanished after update"));
        });
    }

    public Order cancelOrder(int orderId) {
        return JdbcSupport.inTransaction(conn -> {
            Order order = orderRepository.findById(orderId, conn)
                    .orElseThrow(() -> ApiException.notFound("Order not found: " + orderId));
            requirePending(order, "cancelled");
            orderRepository.updateStatus(orderId, OrderStatus.CANCELLED, conn);
            return orderRepository.findById(orderId, conn)
                    .orElseThrow(() -> new DataAccessException("Order vanished after update"));
        });
    }

    public Order getOrder(int id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Order not found: " + id));
    }

    public List<Order> getUserOrders(int userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found: " + userId));
        return orderRepository.findByUserId(userId);
    }

    private static void requirePending(Order order, String action) {
        if (order.getStatus() != OrderStatus.PENDING)
            throw ApiException.conflict("Only PENDING orders can be " + action
                    + " (order is " + order.getStatus() + ")");
    }
}