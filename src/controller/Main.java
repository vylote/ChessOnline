package controller;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Luôn chạy giao diện người dùng trên Event Dispatch Thread (EDT) để đảm bảo an toàn luồng
        SwingUtilities.invokeLater(() -> {
            // Khởi tạo Controller.
            // Constructor của GameController sẽ tự động:
            // 1. Tạo MainFrame (Cửa sổ duy nhất)
            // 2. Nạp MenuPanel vào cửa sổ đó
            // 3. Hiển thị cửa sổ ra màn hình
            new GameController();
        });
    }
}