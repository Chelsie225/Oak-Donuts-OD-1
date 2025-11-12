package oakdonuts.models;

/**
 * Header: OrderItem.java
 * Model: single line item on an order
 */
public class OrderItem {
    private int orderItemId;
    private int orderId;
    private MenuItem menuItem;
    private int quantity;
    private double linePrice;

    public OrderItem() {}

    public OrderItem(MenuItem menuItem, int quantity) {
        this.menuItem = menuItem;
        this.quantity = quantity;
        this.linePrice = menuItem.getPrice() * quantity;
    }

    // getters/setters
    public int getOrderItemId() { return orderItemId; }
    public void setOrderItemId(int orderItemId) { this.orderItemId = orderItemId; }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public MenuItem getMenuItem() { return menuItem; }
    public void setMenuItem(MenuItem menuItem) { this.menuItem = menuItem; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
        this.linePrice = (menuItem != null) ? menuItem.getPrice() * quantity : 0.0;
    }

    public double getLinePrice() { return linePrice; }
}
