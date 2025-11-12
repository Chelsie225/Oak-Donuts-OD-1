package oakdonuts.models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Header: Order.java
 * Purpose: holds order-level information and order items
 */
public class Order {
    private int orderId;
    private String transactionId;
    private Timestamp orderDate;
    private double total;
    private List<OrderItem> items = new ArrayList<>();

    // Constructors
    public Order() {}

    public Order(String transactionId, Timestamp orderDate) {
        this.transactionId = transactionId;
        this.orderDate = orderDate;
    }

    // Methods
    public void addItem(OrderItem item) {
        items.add(item);
        recalcTotal();
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        recalcTotal();
    }

    public void recalcTotal() {
        total = 0.0;
        for (OrderItem oi : items) total += oi.getLinePrice();
    }

    // Getters/setters
    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public Timestamp getOrderDate() { return orderDate; }
    public void setOrderDate(Timestamp orderDate) { this.orderDate = orderDate; }

    public double getTotal() { return total; }
    public List<OrderItem> getItems() { return items; }
}
