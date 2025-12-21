package view;

import controller.GameController;
import utility.AppConstants;
import javax.swing.*;
import java.awt.image.BufferedImage;

public class MenuPauseFrame extends JFrame {

    private final GameController controller;
    private final MenuPanel menuPanel;
    private final PausePanel pausePanel;

    public MenuPauseFrame(GameController controller) {
        this.controller = controller;
        setTitle("Chess Game - Menu/Pause");
        setSize(AppConstants.WINDOW_WIDTH, AppConstants.WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        this.menuPanel = new MenuPanel(controller, this);
        this.pausePanel = new PausePanel(controller, this);

        // 2. Thiết lập Frame chứa MenuPanel ban đầu
        setMenuPanel();

        // 3. Thiết lập tham chiếu trong Controller
        controller.setUiFrame(this);
    }

    /** Chuyển sang hiển thị MenuPanel */
    public void setMenuPanel() {
        setContentPane(menuPanel);
        revalidate(); // Tái hợp lệ hóa layout
        repaint();
    }

    public void setPausePanel(BufferedImage snapshot) {
        // **QUAN TRỌNG:** Truyền ảnh cho PausePanel trước khi đặt nó làm Content Pane
        pausePanel.setBackgroundSnapshot(snapshot);

        setContentPane(pausePanel);
        revalidate();
        repaint();
    }
}