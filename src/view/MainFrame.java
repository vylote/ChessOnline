package view;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * MainFrame là cửa sổ duy nhất của trò chơi.
 * Nó không cố định một Panel nào mà sẽ thay đổi nội dung theo điều phối của Controller.
 */
public class MainFrame extends JFrame {

    public MainFrame() {
        // Thiết lập các thuộc tính cơ bản của cửa sổ
        setTitle("Chess Game 2025");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false); // Ngăn việc thay đổi kích thước làm hỏng layout

        // Không gọi setSize ở đây vì pack() sẽ tự tính toán dựa trên Panel bên trong
    }

    /**
     * Hàm quan trọng nhất: Thay thế Panel hiện tại bằng một Panel mới.
     * @param panel Panel cần hiển thị (MenuPanel, GamePanel, hoặc PausePanel)
     */
    public void showPanel(JPanel panel) {
        // 1. Loại bỏ toàn bộ nội dung cũ
        getContentPane().removeAll();

        // 2. Thêm Panel mới vào
        add(panel);

        // 3. Ép Frame ôm khít theo kích thước PreferredSize của Panel (ví dụ 1200x800)
        pack();

        // 4. Căn giữa cửa sổ trên màn hình
        setLocationRelativeTo(null);

        // 5. Đảm bảo hiển thị và cập nhật lại giao diện
        setVisible(true);
        revalidate();
        repaint();

        // 6. Yêu cầu Focus để Panel mới có thể nhận sự kiện phím/chuột ngay lập tức
        panel.requestFocusInWindow();
    }
}