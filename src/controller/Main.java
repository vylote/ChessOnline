package controller;

import javax.swing.SwingUtilities;
import view.MainFrame;
import view.MenuPauseFrame;

public class Main {
    public static void main(String[] args) {

        // Luôn chạy giao diện người dùng trên Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            GameController gc = new GameController();
            MainFrame mainFrame = new MainFrame(gc);
            MenuPauseFrame uiFrame = new MenuPauseFrame(gc);
            uiFrame.setVisible(true);
            gc.launchGame(mainFrame.getGamePanel());
        });
    }
}