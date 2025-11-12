package oakdonuts;

import oakdonuts.models.MenuItem;
import oakdonuts.models.Order;
import oakdonuts.models.OrderItem;
import oakdonuts.utils.DateUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Header: DBHelper.java
 * Responsible for all DB interactions (CRUD) using embedded Derby
 *
 * NOTE: Derby embedded driver is used (org.apache.derby.jdbc.EmbeddedDriver).
 * If your Java distribution requires extra library, add derby.jar to classpath.
 */
public class DBHelper {
    private static final String DB_URL = "jdbc:derby:oddb;create=true";

    private Connection conn;

    /** Initialize connection and create tables if needed */
    public DBHelper() throws SQLException {
        conn = DriverManager.getConnection(DB_URL);
        createTablesIfNotExist();
    }

    /** Close connection */
    public void close() {
        try {
            if (conn != null && !conn.isClosed()) conn.close();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /** Create schema tables if not present */
    private void createTablesIfNotExist() throws SQLException {
        Statement st = conn.createStatement();

        // Create menu_items
        st.executeUpdate(
            "CREATE TABLE menu_items (" +
            "item_id INTEGER NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
            "name VARCHAR(100)," +
            "price DECIMAL(6,2)," +
            "description VARCHAR(255))"
        );
        // create orders
        st.executeUpdate(
            "CREATE TABLE orders (" +
            "order_id INTEGER NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY," +
            "transaction_id VARCHAR(50) UNIQUE," +
            "order_date TIMESTAMP," +
            "total DECIMAL(8,2))"
        );
        // create order_items
        st.executeUpdate(
            "CREATE TABLE order_items (" +
            "order_item_id INTEGER NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY," +
            "order_id INTEGER," +
            "item_id INTEGER," +
            "quantity INTEGER," +
            "line_price DECIMAL(8,2)," +
            "CONSTRAINT fk_order FOREIGN KEY (order_id) REFERENCES orders(order_id)," +
            "CONSTRAINT fk_item FOREIGN KEY (item_id) REFERENCES menu_items(item_id))"
        );
        st.close();

        // If first run, insert sample menu items
        if (getAllMenuItems().isEmpty()) {
            insertMenuItem(new MenuItem(0, "Glazed Donut", 1.50, "Classic glazed donut"));
            insertMenuItem(new MenuItem(0, "Chocolate Frosted", 1.75, "Chocolate icing"));
            insertMenuItem(new MenuItem(0, "Sprinkles", 1.85, "Fun colorful sprinkles"));
        }
    }

    /* ------------------ Menu Items CRUD ------------------ */

    /** Insert a new menu item into DB */
    public void insertMenuItem(MenuItem m) throws SQLException {
        String sql = "INSERT INTO menu_items (name, price, description) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.getName());
            ps.setDouble(2, m.getPrice());
            ps.setString(3, m.getDescription());
            ps.executeUpdate();
        }
    }

    /** Update an existing menu item */
    public void updateMenuItem(MenuItem m) throws SQLException {
        String sql = "UPDATE menu_items SET name=?, price=?, description=? WHERE item_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.getName());
            ps.setDouble(2, m.getPrice());
            ps.setString(3, m.getDescription());
            ps.setInt(4, m.getItemId());
            ps.executeUpdate();
        }
    }

    /** Delete menu item by ID */
    public void deleteMenuItem(int itemId) throws SQLException {
        String sql = "DELETE FROM menu_items WHERE item_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            ps.executeUpdate();
        }
    }

    /** Read all menu items */
    public List<MenuItem> getAllMenuItems() throws SQLException {
        List<MenuItem> list = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT item_id, name, price, description FROM menu_items")) {
            while (rs.next()) {
                MenuItem m = new MenuItem(
                    rs.getInt("item_id"),
                    rs.getString("name"),
                    rs.getDouble("price"),
                    rs.getString("description")
                );
                list.add(m);
            }
        } catch (SQLException e) {
            // If table missing, return empty list (table creation handled elsewhere)
        }
        return list;
    }

    /* ------------------ Orders CRUD ------------------ */

    /** Save a full order (orders + order_items). Handles insert or update. */
    public void saveOrder(Order order) throws SQLException {
        conn.setAutoCommit(false);
        try {
            if (order.getOrderId() == 0) {
                // Insert order
                String ins = "INSERT INTO orders (transaction_id, order_date, total) VALUES (?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, order.getTransactionId());
                    ps.setTimestamp(2, order.getOrderDate());
                    ps.setDouble(3, order.getTotal());
                    ps.executeUpdate();
                    ResultSet keys = ps.getGeneratedKeys();
                    if (keys.next()) {
                        int orderId = keys.getInt(1);
                        order.setOrderId(orderId);
                    }
                }
            } else {
                String upd = "UPDATE orders SET transaction_id=?, order_date=?, total=? WHERE order_id=?";
                try (PreparedStatement ps = conn.prepareStatement(upd)) {
                    ps.setString(1, order.getTransactionId());
                    ps.setTimestamp(2, order.getOrderDate());
                    ps.setDouble(3, order.getTotal());
                    ps.setInt(4, order.getOrderId());
                    ps.executeUpdate();
                }
                // remove existing order_items for update (simple approach)
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM order_items WHERE order_id=?")) {
                    ps.setInt(1, order.getOrderId());
                    ps.executeUpdate();
                }
            }

            // Insert order items
            String insItem = "INSERT INTO order_items (order_id, item_id, quantity, line_price) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insItem)) {
                for (OrderItem oi : order.getItems()) {
                    ps.setInt(1, order.getOrderId());
                    ps.setInt(2, oi.getMenuItem().getItemId());
                    ps.setInt(3, oi.getQuantity());
                    ps.setDouble(4, oi.getLinePrice());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /** Delete order and its items */
    public void deleteOrder(int orderId) throws SQLException {
        conn.setAutoCommit(false);
        try {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM order_items WHERE order_id=?")) {
                ps.setInt(1, orderId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM orders WHERE order_id=?")) {
                ps.setInt(1, orderId);
                ps.executeUpdate();
            }
            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /** Read all orders (basic info) */
    public List<Order> getAllOrders() throws SQLException {
        List<Order> list = new ArrayList<>();
        String q = "SELECT order_id, transaction_id, order_date, total FROM orders ORDER BY order_date DESC";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(q)) {
            while (rs.next()) {
                Order o = new Order();
                o.setOrderId(rs.getInt("order_id"));
                o.setTransactionId(rs.getString("transaction_id"));
                o.setOrderDate(rs.getTimestamp("order_date"));
                o.recalcTotal();
                list.add(o);
            }
        }
        return list;
    }

    /** Load items for a given order */
    public List<OrderItem> getOrderItems(int orderId) throws SQLException {
        List<OrderItem> list = new ArrayList<>();
        String q = "SELECT oi.order_item_id, oi.quantity, oi.line_price, mi.item_id, mi.name, mi.price, mi.description " +
                   "FROM order_items oi JOIN menu_items mi ON oi.item_id = mi.item_id WHERE oi.order_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MenuItem m = new MenuItem(
                        rs.getInt("item_id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getString("description")
                    );
                    OrderItem oi = new OrderItem(m, rs.getInt("quantity"));
                    oi.setOrderItemId(rs.getInt("order_item_id"));
                    oi.setOrderId(orderId);
                    list.add(oi);
                }
            }
        }
        return list;
    }

    /* ------------------ Utility: transaction id generation ------------------ */

    /**
     * Generate next transaction sequence for today's date (simple approach:
     * counts how many orders have today's date prefix)
     */
    public String generateNextTransactionId() throws SQLException {
        String todayPrefix = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        String like = "OD-" + todayPrefix + "%";
        String q = "SELECT COUNT(*) AS cnt FROM orders WHERE transaction_id LIKE ?";
        try (PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setString(1, like);
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                if (rs.next()) count = rs.getInt("cnt");
                // next sequence = count+1
                return DateUtils.generateTransactionId(count + 1);
            }
        }
    }
}
