package repository.Impl;

import model.Order;
import model.OrderItem;
import model.Enums.OrderStatus;
import repository.JdbcSupport;
import repository.OrderRepository;
import util.DataAccessException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OrderRepositoryImpl implements OrderRepository {
    private static final String SELECT =
            "SELECT order_id, user_id, total_amount, status, created_at FROM orders ";

    private static Order mapOrder(ResultSet rs) throws SQLException {
        return new Order(
                rs.getInt("order_id"),
                rs.getInt("user_id"),
                rs.getDouble("total_amount"),
                OrderStatus.valueOf(rs.getString("status")),
                JdbcSupport.toLocalDateTime(rs.getTimestamp("created_at")),
                new ArrayList<>());
    }

    private static OrderItem mapItem(ResultSet rs) throws SQLException {
        return new OrderItem(
                rs.getInt("order_item_id"),
                rs.getInt("order_id"),
                rs.getInt("product_id"),
                rs.getInt("quantity"),
                rs.getDouble("unit_price"));
    }

    private List<OrderItem> findItems(Connection c, int orderId) {
        return JdbcSupport.query(c,
                "SELECT order_item_id, order_id, product_id, quantity, unit_price "
                        + "FROM order_items WHERE order_id = ? ORDER BY order_item_id",
                ps -> ps.setInt(1, orderId), OrderRepositoryImpl::mapItem);
    }

    private Order withItems(Connection c, Order o) {
        o.setItems(findItems(c, o.getId()));
        return o;
    }

    @Override
    public Order save(Order order) {
        return JdbcSupport.inTransaction(c -> save(order, c));
    }

    @Override
    public Order save(Order order, Connection c) {
        int orderId = JdbcSupport.insert(c,
                "INSERT INTO orders (user_id, total_amount, status) VALUES (?, ?, ?)",
                ps -> {
                    ps.setInt(1, order.getUserId());
                    ps.setDouble(2, order.getTotalAmount());
                    ps.setString(3, order.getStatus().name());
                });
        order.setId(orderId);

        List<OrderItem> saved = order.getItems().stream()
                .map(i -> new OrderItem(
                        JdbcSupport.insert(c,
                                "INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)",
                                ps -> {
                                    ps.setInt(1, orderId);
                                    ps.setInt(2, i.productId());
                                    ps.setInt(3, i.quantity());
                                    ps.setDouble(4, i.unitPrice());
                                }),
                        orderId, i.productId(), i.quantity(), i.unitPrice()))
                .collect(Collectors.toCollection(ArrayList::new));
        order.setItems(saved);

        return findById(orderId, c).orElseThrow(() -> new DataAccessException("Order vanished after insert"));
    }

    @Override
    public Optional<Order> findById(int id) {
        return JdbcSupport.withConnection(c ->
                JdbcSupport.queryOne(c, SELECT + "WHERE order_id = ?", ps -> ps.setInt(1, id), OrderRepositoryImpl::mapOrder)
                        .map(o -> withItems(c, o)));
    }

    @Override
    public Optional<Order> findById(int id, Connection c) {
        return JdbcSupport.queryOne(c, SELECT + "WHERE order_id = ? FOR UPDATE", ps -> ps.setInt(1, id), OrderRepositoryImpl::mapOrder)
                .map(o -> withItems(c, o));
    }

    @Override
    public List<Order> findByUserId(int userId) {
        return JdbcSupport.withConnection(c ->
                JdbcSupport.query(c, SELECT + "WHERE user_id = ? ORDER BY order_id DESC",
                                ps -> ps.setInt(1, userId), OrderRepositoryImpl::mapOrder)
                        .stream().map(o -> withItems(c, o)).collect(Collectors.toList()));
    }

    @Override
    public List<Order> findAll() {
        return JdbcSupport.withConnection(c ->
                JdbcSupport.query(c, SELECT + "ORDER BY order_id DESC",
                                JdbcSupport.NO_PARAMS, OrderRepositoryImpl::mapOrder)
                        .stream().map(o -> withItems(c, o)).collect(Collectors.toList()));
    }

    @Override
    public boolean updateStatus(int id, OrderStatus status) {
        return JdbcSupport.withConnection(c -> updateStatus(id, status, c));
    }

    @Override
    public boolean updateStatus(int id, OrderStatus status, Connection c) {
        return JdbcSupport.update(c, "UPDATE orders SET status = ? WHERE order_id = ?", ps -> {
            ps.setString(1, status.name());
            ps.setInt(2, id);
        }) > 0;
    }

    @Override
    public boolean delete(int id) {
        return JdbcSupport.inTransaction(c -> {
            JdbcSupport.update(c, "DELETE FROM order_items WHERE order_id = ?", ps -> ps.setInt(1, id));
            return JdbcSupport.update(c, "DELETE FROM orders WHERE order_id = ?", ps -> ps.setInt(1, id)) > 0;
        });
    }
}