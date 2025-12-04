package view;

import controller.GameController;
import javax.swing.JFrame;
import utility.AppConstants; // Lớp chứa WINDOW_WIDTH/HEIGHT

public class MainFrame extends JFrame {

    private final GamePanel gamePanel;

    public MainFrame(GameController controller) {
        setTitle("Chess Game");
        setSize(AppConstants.WINDOW_WIDTH, AppConstants.WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // 1. Chỉ định MainFrame cho Controller
        controller.setMainFrame(this);

        // 2. Tạo GamePanel (View) và thêm vào Frame
        this.gamePanel = new GamePanel(controller);
        add(gamePanel);

        pack(); // Đảm bảo kích thước Frame khớp với PreferredSize của GamePanel

        // Ban đầu, MainFrame phải ẩn đi (vì MenuFrame sẽ được hiển thị trước)
        setVisible(false);
    }

    // Getter cần thiết để Main.java có thể khởi chạy game loop
    public GamePanel getGamePanel() {
        return gamePanel;
    }
}