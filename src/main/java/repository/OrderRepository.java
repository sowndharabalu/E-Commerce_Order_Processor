package repository;

import model.Order;
import model.Enums.OrderStatus;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);

    Order save(Order order, Connection conn);

    Optional<Order> findById(int id);

    Optional<Order> findById(int id, Connection conn);

    List<Order> findByUserId(int userId);

    List<Order> findAll();

    boolean updateStatus(int id, OrderStatus status);

    boolean updateStatus(int id, OrderStatus status, Connection conn);

    boolean delete(int id);
}
