package model;


public record OrderItem(Integer id, Integer orderId, Integer productId, int quantity, double unitPrice) {
    public static OrderItem createOrderItem(Integer id, Integer orderId, Integer productId, Integer quantity, double unitPrice) {
        return new OrderItem(id, orderId, productId, quantity, unitPrice);
    }

    public double getTotalPrice() {
        return quantity * unitPrice;
    }
}