package controller;

import javax.swing.JFrame;

import view.GamePanel;

public class Main {
    public static void main(String[] args) {
        JFrame window = new JFrame("Chess");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);

        // Khởi tạo Controller
        GameController gc = new GameController();

        // Khởi tạo View và truyền Controller vào
        GamePanel gp = new GamePanel(gc);

        window.add(gp);
        window.pack();

        window.setLocationRelativeTo(null); // set up the screen at the center of your monitor
        window.setVisible(true);

        // Khởi chạy game từ Controller
        gc.launchGame(gp);
    }
}