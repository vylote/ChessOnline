package view;

import controller.GameController;
import java.awt.image.BufferedImage;
import javax.swing.*;

public class MenuPauseFrame extends JFrame {
    private final MenuPanel menuPanel;
    private final PausePanel pausePanel;

    public MenuPauseFrame(GameController gc) {
        setTitle("Chess Game 2025");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        this.menuPanel = new MenuPanel(gc, this);
        this.pausePanel = new PausePanel(gc, this);

        setContentPane(menuPanel);
        gc.setUiFrame(this);
    }

    public void setMenuPanel() {
        setContentPane(menuPanel);
        revalidate(); repaint();
    }

    public void setPausePanel(BufferedImage snapshot) {
        pausePanel.setBackgroundSnapshot(snapshot); // Sẽ không còn lỗi "cannot find symbol"
        setContentPane(pausePanel);
        revalidate(); repaint();
    }
}