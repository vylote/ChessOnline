package controller;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Khởi tạo đầu não điều khiển game
                new GameController();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}