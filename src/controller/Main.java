package controller;

import javax.swing.SwingUtilities; // Cần import SwingUtilities
import view.MainFrame;           // Cần import MainFrame
import view.MenuPauseFrame;

public class Main {
    public static void main(String[] args) {

        // Luôn chạy giao diện người dùng trên Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {

            // 1. Khởi tạo Controller (Model Logic)
            GameController gc = new GameController();

            // 2. Khởi tạo MainFrame (Cửa sổ Game chính, ban đầu ẩn)
            // Lớp này chứa GamePanel và được Controller điều khiển.
            MainFrame mainFrame = new MainFrame(gc); // Tự động setVisible(false) bên trong

            MenuPauseFrame uiFrame = new MenuPauseFrame(gc);
            uiFrame.setVisible(true);
            gc.launchGame(mainFrame.getGamePanel());
        });
    }
}