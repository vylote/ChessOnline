package view;

import controller.GameController;
import model.GameState;
import model.State;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PausePanel {

    // Kích thước cửa sổ
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;

    private final GameController controller;
    private final Rectangle resumeButton;
    private final Rectangle saveButton;
    private final Rectangle exitButton;

    // --- HẰNG SỐ VẼ PANEL ---
    private static final int PANEL_W = 350;
    private static final int PANEL_H = 400;
    private static final int BUTTON_W = 250;
    private static final int BUTTON_H = 60; // Tăng kích thước nút cho đồng bộ

    // TÍNH TOÁN CĂN GIỮA TOÀN BỘ CỬA SỔ
    private static final int CENTER_X = (WINDOW_WIDTH - PANEL_W) / 2;
    private static final int CENTER_Y = (WINDOW_HEIGHT - PANEL_H) / 2;

    // Vị trí X của nút (căn giữa trong PANEL)
    private static final int BUTTON_DRAW_X = CENTER_X + (PANEL_W - BUTTON_W) / 2;


    public PausePanel(GameController gc, GamePanel gp) {
        this.controller = gc;

        // Định vị Y của các nút
        int startButtonY = CENTER_Y + 100;

        // SỬA LỖI: Sử dụng tọa độ X đã tính toán (BUTTON_DRAW_X)
        resumeButton = new Rectangle(BUTTON_DRAW_X, startButtonY, BUTTON_W, BUTTON_H);
        saveButton = new Rectangle(BUTTON_DRAW_X, startButtonY + BUTTON_H + 10, BUTTON_W, BUTTON_H);
        exitButton = new Rectangle(BUTTON_DRAW_X, startButtonY + 2 * (BUTTON_H + 10), BUTTON_W, BUTTON_H);

        // ... (Giữ nguyên Logic Xử lý Input chuột)
        gp.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (GameState.currentState == State.PAUSED) {
                    int x = e.getX();
                    int y = e.getY();

                    if (resumeButton.contains(x, y)) {
                        controller.resumeGame(); // Quay lại game
                    } else if (saveButton.contains(x, y)) {
                        System.out.println("Save Game clicked (TBD)");
                    } else if (exitButton.contains(x, y)) {
                        System.out.println("Exit to Menu clicked (TBD)");
                        GameState.setState(State.MENU); // Quay lại Menu
                        gp.repaint();
                    }
                }
            }
        });
    }

    // --- PHƯƠNG THỨC VẼ ---
    public void draw(Graphics2D g2) {
        // 1. Vẽ nền mờ che TOÀN BỘ cửa sổ
        g2.setColor(new Color(2, 2, 2, 200));
        g2.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        // 2. Vẽ khung cửa sổ Pause ở giữa
        g2.setColor(new Color(40, 40, 40));
        g2.fillRect(CENTER_X, CENTER_Y, PANEL_W, PANEL_H);

        // 3. Vẽ tiêu đề (Sử dụng FontMetrics để căn giữa)
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 40));
        String title = "PAUSED";
        FontMetrics fm = g2.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        // Căn giữa theo PANEL_W
        int titleX = CENTER_X + (PANEL_W - titleWidth) / 2;
        g2.drawString(title, titleX, CENTER_Y + 60); // Vị trí Y cố định

        // 4. Vẽ các nút
        g2.setFont(new Font("Arial", Font.PLAIN, 24));

        drawButton(g2, resumeButton, "CONTINUE", Color.GREEN.darker());
        drawButton(g2, saveButton, "SAVE GAME", Color.YELLOW.darker());
        drawButton(g2, exitButton, "EXIT TO MENU", Color.RED.darker());
    }

    private void drawButton(Graphics2D g2, Rectangle rect, String text, Color color) {
        g2.setColor(color);
        g2.fill(rect);
        g2.setColor(Color.WHITE);
        g2.draw(rect);

        FontMetrics fm = g2.getFontMetrics();
        int textX = rect.x + (rect.width - fm.stringWidth(text)) / 2;
        int textY = rect.y + ((rect.height - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(text, textX, textY);
    }
}