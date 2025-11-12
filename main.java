package oakdonuts;

import javax.swing.*;

/**
 * Header: MainApp.java
 * Entrypoint for Oak Donuts OD project
 */
public class MainApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                DBHelper db = new DBHelper();
                DonutShopGUI gui = new DonutShopGUI(db);
                gui.setVisible(true);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Failed to start application: " + ex.getMessage());
            }
        });
    }
}
