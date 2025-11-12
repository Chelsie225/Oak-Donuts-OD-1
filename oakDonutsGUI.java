package oakdonuts;

import oakdonuts.models.MenuItem;
import oakdonuts.models.Order;
import oakdonuts.models.OrderItem;
import oakdonuts.utils.DateUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Header: DonutShopGUI.java
 * Purpose: Swing GUI for Oak Donuts — supports menu CRUD and order CRUD.
 *
 * This is a single-file Swing GUI for demonstration/project submission.
 */
public class DonutShopGUI extends JFrame {
    private DBHelper db;
    private DefaultListModel<MenuItem> menuListModel = new DefaultListModel<>();
    private JList<MenuItem> menuList;

    // Current order UI components
    private DefaultTableModel orderTableModel;
    private JTable orderTable;
    private Order currentOrder;

    // Orders list (existing orders)
    private DefaultTableModel ordersModel;
    private JTable ordersTable;

    public DonutShopGUI(DBHelper db) {
        this.db = db;
        setTitle("Oak Donuts - OD");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);

        initComponents();
        loadMenuItems();
        loadOrders();
    }

    /** Initialize GUI components and layout */
    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(10,10,10,10));
        setContentPane(root);

        // Left: menu items management
        JPanel left = new JPanel(new BorderLayout(5,5));
        left.setPreferredSize(new Dimension(300, 0));
        left.setBorder(BorderFactory.createTitledBorder("Menu Items"));
        menuList = new JList<>(menuListModel);
        menuList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        left.add(new JScrollPane(menuList), BorderLayout.CENTER);

        JPanel menuButtons = new JPanel(new GridLayout(1,3,5,5));
        JButton addMenuBtn = new JButton("Add");
        JButton editMenuBtn = new JButton("Edit");
        JButton delMenuBtn = new JButton("Delete");
        menuButtons.add(addMenuBtn);
        menuButtons.add(editMenuBtn);
        menuButtons.add(delMenuBtn);
        left.add(menuButtons, BorderLayout.SOUTH);

        addMenuBtn.addActionListener(e -> showMenuItemDialog(null));
        editMenuBtn.addActionListener(e -> {
            MenuItem sel = menuList.getSelectedValue();
            if (sel != null) showMenuItemDialog(sel);
            else JOptionPane.showMessageDialog(this, "Select a menu item to edit.");
        });
        delMenuBtn.addActionListener(e -> {
            MenuItem sel = menuList.getSelectedValue();
            if (sel != null) {
                int ok = JOptionPane.showConfirmDialog(this, "Delete " + sel.getName() + "?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (ok == JOptionPane.YES_OPTION) {
                    try { db.deleteMenuItem(sel.getItemId()); loadMenuItems(); } catch (SQLException ex) { showError(ex); }
                }
            }
        });

        // Center: Order area
        JPanel center = new JPanel(new BorderLayout(5,5));
        center.setBorder(BorderFactory.createTitledBorder("Order Area"));

        // top: add item to current order
        JPanel addItemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JComboBox<MenuItem> cbMenu = new JComboBox<>();
        JTextField qtyField = new JTextField("1", 3);
        JButton addToOrderBtn = new JButton("Add to Order");
        addItemPanel.add(new JLabel("Item:"));
        addItemPanel.add(cbMenu);
        addItemPanel.add(new JLabel("Qty:"));
        addItemPanel.add(qtyField);
        addItemPanel.add(addToOrderBtn);
        center.add(addItemPanel, BorderLayout.NORTH);

        // middle: order table
        orderTableModel = new DefaultTableModel(new Object[]{"Item","Qty","Line Price"},0) {
            public boolean isCellEditable(int r,int c){return false;}
        };
        orderTable = new JTable(orderTableModel);
        center.add(new JScrollPane(orderTable), BorderLayout.CENTER);

        // bottom: save / new / delete buttons
        JPanel orderActions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton newOrderBtn = new JButton("New Order");
        JButton saveOrderBtn = new JButton("Save Order");
        JButton delOrderBtn = new JButton("Delete Order");
        JLabel totalLabel = new JLabel("Total: $0.00");
        orderActions.add(newOrderBtn);
        orderActions.add(saveOrderBtn);
        orderActions.add(delOrderBtn);
        orderActions.add(totalLabel);
        center.add(orderActions, BorderLayout.SOUTH);

        // Right: existing orders
        JPanel right = new JPanel(new BorderLayout(5,5));
        right.setPreferredSize(new Dimension(350,0));
        right.setBorder(BorderFactory.createTitledBorder("Saved Orders"));
        ordersModel = new DefaultTableModel(new Object[]{"OrderId","Transaction","Date","Total"},0) {
            public boolean isCellEditable(int r,int c){return false;}
        };
        ordersTable = new JTable(ordersModel);
        right.add(new JScrollPane(ordersTable), BorderLayout.CENTER);
        JButton loadOrderBtn = new JButton("Load Selected");
        right.add(loadOrderBtn, BorderLayout.SOUTH);

        // Layout add
        root.add(left, BorderLayout.WEST);
        root.add(center, BorderLayout.CENTER);
        root.add(right, BorderLayout.EAST);

        // behavior wiring
        // Fill combo box from menu list
        menuListModel.addListDataListener(new javax.swing.event.ListDataListener() {
            public void intervalAdded(javax.swing.event.ListDataEvent e) { syncCombo(); }
            public void intervalRemoved(javax.swing.event.ListDataEvent e) { syncCombo(); }
            public void contentsChanged(javax.swing.event.ListDataEvent e) { syncCombo(); }
        });

        addToOrderBtn.addActionListener(e -> {
            MenuItem mi = (MenuItem) cbMenu.getSelectedItem();
            if (mi == null) { JOptionPane.showMessageDialog(this, "No item selected."); return; }
            int qty;
            try { qty = Integer.parseInt(qtyField.getText()); if (qty <= 0) throw new NumberFormatException(); }
            catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Enter a valid quantity."); return; }
            OrderItem oi = new OrderItem(mi, qty);
            if (currentOrder == null) currentOrder = new Order();
            currentOrder.addItem(oi);
            refreshCurrentOrderTable(totalLabel);
        });

        newOrderBtn.addActionListener(e -> {
            currentOrder = new Order();
            currentOrder.setTransactionId("TEMP");
            currentOrder.setOrderDate(DateUtils.now());
            orderTableModel.setRowCount(0);
            totalLabel.setText("Total: $0.00");
        });

        saveOrderBtn.addActionListener(e -> {
            if (currentOrder == null || currentOrder.getItems().isEmpty()) {
                JOptionPane.showMessageDialog(this, "No items in order.");
                return;
            }
            try {
                // if first time saving, generate transaction id
                if (currentOrder.getOrderId() == 0) {
                    currentOrder.setOrderDate(DateUtils.now());
                    currentOrder.setTransactionId(db.generateNextTransactionId());
                }
                currentOrder.recalcTotal();
                db.saveOrder(currentOrder);
                JOptionPane.showMessageDialog(this, "Order saved: " + currentOrder.getTransactionId());
                loadOrders();
            } catch (SQLException ex) { showError(ex); }
        });

        delOrderBtn.addActionListener(e -> {
            int sel = ordersTable.getSelectedRow();
            if (sel >= 0) {
                int orderId = (int) ordersModel.getValueAt(sel, 0);
                int ok = JOptionPane.showConfirmDialog(this, "Delete order id " + orderId + "?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (ok == JOptionPane.YES_OPTION) {
                    try { db.deleteOrder(orderId); loadOrders(); } catch (SQLException ex) { showError(ex); }
                }
            } else JOptionPane.showMessageDialog(this, "Select an order to delete.");
        });

        loadOrderBtn.addActionListener(e -> {
            int sel = ordersTable.getSelectedRow();
            if (sel >= 0) {
                int orderId = (int) ordersModel.getValueAt(sel, 0);
                try {
                    Order o = new Order();
                    List<OrderItem> items = db.getOrderItems(orderId);
                    o.setOrderId(orderId);
                    o.setTransactionId((String) ordersModel.getValueAt(sel, 1));
                    o.setOrderDate(new java.sql.Timestamp(System.currentTimeMillis())); // date in table already
                    for (OrderItem oi : items) o.addItem(oi);
                    currentOrder = o;
                    refreshCurrentOrderTable(totalLabel);
                } catch (SQLException ex) { showError(ex); }
            } else JOptionPane.showMessageDialog(this, "Select an order to load.");
        });

        // helper to sync combo box
        syncCombo();
    }

    /** Show dialog to add or edit a menu item */
    private void showMenuItemDialog(MenuItem existing) {
        JTextField nameF = new JTextField(existing != null ? existing.getName() : "");
        JTextField priceF = new JTextField(existing != null ? String.valueOf(existing.getPrice()) : "0.00");
        JTextField descF = new JTextField(existing != null ? existing.getDescription() : "");
        Object[] fields = {
            "Name:", nameF,
            "Price:", priceF,
            "Description:", descF
        };
        int ok = JOptionPane.showConfirmDialog(this, fields, (existing==null?"Add Menu Item":"Edit Menu Item"), JOptionPane.OK_CANCEL_OPTION);
        if (ok == JOptionPane.OK_OPTION) {
            try {
                double price = Double.parseDouble(priceF.getText());
                if (existing == null) {
                    MenuItem m = new MenuItem(0, nameF.getText(), price, descF.getText());
                    db.insertMenuItem(m);
                } else {
                    existing.setName(nameF.getText());
                    existing.setPrice(price);
                    existing.setDescription(descF.getText());
                    db.updateMenuItem(existing);
                }
                loadMenuItems();
            } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Enter a valid price."); }
            catch (SQLException ex) { showError(ex); }
        }
    }

    /** Refresh the combo box for adding items from the menu */
    private void syncCombo() {
        Component[] comps = getContentPane().getComponents();
        // update combo boxes by searching for JComboBox (simple approach)
        JComboBox<MenuItem> cb = findCombo(this);
        if (cb != null) {
            cb.removeAllItems();
            for (int i = 0; i < menuListModel.size(); i++) cb.addItem(menuListModel.getElementAt(i));
        }
    }

    private JComboBox<MenuItem> findCombo(Component c) {
        if (c instanceof JComboBox) return (JComboBox<MenuItem>) c;
        if (c instanceof Container) {
            for (Component ch : ((Container)c).getComponents()) {
                JComboBox<MenuItem> r = findCombo(ch);
                if (r != null) return r;
            }
        }
        return null;
    }

    /** Load menu items from DB into list model */
    private void loadMenuItems() {
        menuListModel.removeAllElements();
        try {
            List<MenuItem> items = db.getAllMenuItems();
            for (MenuItem m : items) menuListModel.addElement(m);
        } catch (SQLException ex) { showError(ex); }
    }

    /** Load saved orders into orders table */
    private void loadOrders() {
        ordersModel.setRowCount(0);
        try {
            List<Order> orders = db.getAllOrders();
            for (Order o : orders) {
                ordersModel.addRow(new Object[]{ o.getOrderId(), o.getTransactionId(), o.getOrderDate(), o.getTotal() });
            }
        } catch (SQLException ex) { showError(ex); }
    }

    /** Refresh current order table UI */
    private void refreshCurrentOrderTable(JLabel totalLabel) {
        orderTableModel.setRowCount(0);
        if (currentOrder == null) {
            totalLabel.setText("Total: $0.00");
            return;
        }
        currentOrder.recalcTotal();
        for (OrderItem oi : currentOrder.getItems()) {
            orderTableModel.addRow(new Object[]{ oi.getMenuItem().getName(), oi.getQuantity(), String.format("%.2f", oi.getLinePrice()) });
        }
        totalLabel.setText(String.format("Total: $%.2f", currentOrder.getTotal()));
    }

    private void showError(Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
    }
}
