package oakdonuts.models;

/**
 * Header: MenuItem.java
 * Oak Donuts OD project
 *
 * Purpose: Simple data model for a menu item.
 *
 * Author: YourName
 * Date: 2025-11-12
 */
public class MenuItem {
    private int itemId;
    private String name;
    private double price;
    private String description;

    /** Constructor */
    public MenuItem() {}

    /** Constructor */
    public MenuItem(int itemId, String name, double price, String description) {
        this.itemId = itemId;
        this.name = name;
        this.price = price;
        this.description = description;
    }

    // Getters and setters
    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return name + " ($" + String.format("%.2f", price) + ")";
    }
}
