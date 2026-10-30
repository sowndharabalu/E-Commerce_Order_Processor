package model;

import model.Enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Order {
    private Integer id;
    private final Integer userId;
    private final double totalAmount;
    private final LocalDateTime createdAt;
    private OrderStatus status;
    private List<OrderItem> items;

    public Order(Integer id, Integer userId, double totalAmount, OrderStatus status, LocalDateTime createdAt, List<OrderItem> items) {
        this.id = id;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.status = status;
        this.items = (items == null) ? new ArrayList<>() : items;
    }

    public Integer getId() {
        return id;
    }

    public Integer getUserId() {
        return userId;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Double.compare(totalAmount, order.totalAmount) == 0 && Objects.equals(id, order.id) && Objects.equals(userId, order.userId) && Objects.equals(createdAt, order.createdAt) && status == order.status && Objects.equals(items, order.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, totalAmount, createdAt, status, items);
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", userId=" + userId +
                ", totalAmount=" + totalAmount +
                ", createdAt=" + createdAt +
                ", status=" + status +
                ", items=" + items +
                '}';
    }

    public void setId(int orderId) {
        id = orderId;
    }
}
