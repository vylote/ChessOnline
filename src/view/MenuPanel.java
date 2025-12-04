package view;

import controller.GameController;
import model.GameState;
import model.State;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MenuPanel {

    // Kích thước cửa sổ (Lấy từ GamePanel)
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;

    private final GameController controller;
    private final Rectangle playButton;
    private final Rectangle loadButton;
    private final Rectangle settingsButton;
    private final Rectangle quitButton;

    // --- HẰNG SỐ VẼ ---
    private static final int BUTTON_WIDTH = 250;
    private static final int BUTTON_HEIGHT = 60;
    private static final int BUTTON_SPACING = 20; // Khoảng cách giữa các nút

    // Tính toán CĂN GIỮA TOÀN BỘ
    private static final int CENTER_X = (WINDOW_WIDTH - BUTTON_WIDTH) / 2;
    // Tính toán vị trí Y cho khối nút (căn giữa chiều dọc)
    private static final int TOTAL_HEIGHT = 4 * BUTTON_HEIGHT + 3 * BUTTON_SPACING;
    private static final int START_Y = (WINDOW_HEIGHT - TOTAL_HEIGHT) / 2;


    // --- CONSTRUCTOR (Đã sửa lỗi Dòng 14-18) ---
    public MenuPanel(GameController gc, GamePanel gp) {
        this.controller = gc;

        // Định vị các nút dựa trên CENTER_X và START_Y đã tính toán
        this.playButton = new Rectangle(CENTER_X, START_Y, BUTTON_WIDTH, BUTTON_HEIGHT);
        this.loadButton = new Rectangle(CENTER_X, START_Y + BUTTON_HEIGHT + BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT);
        this.settingsButton = new Rectangle(CENTER_X, START_Y + 2 * (BUTTON_HEIGHT + BUTTON_SPACING), BUTTON_WIDTH, BUTTON_HEIGHT);
        this.quitButton = new Rectangle(CENTER_X, START_Y + 3 * (BUTTON_HEIGHT + BUTTON_SPACING), BUTTON_WIDTH, BUTTON_HEIGHT);

        // --- LOGIC XỬ LÝ CLICK ---
        gp.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (GameState.currentState == State.MENU) {
                    int x = e.getX();
                    int y = e.getY();

                    if (playButton.contains(x, y)) {
                        controller.startNewGame();
                    } else if (loadButton.contains(x, y)) {
                        System.out.println("Load Game clicked (TBD)");
                    } else if (settingsButton.contains(x, y)) {
                        System.out.println("Settings clicked (TBD)");
                    } else if (quitButton.contains(x, y)) {
                        System.exit(0);
                    }
                }
            }
        });
    }

    // --- PHƯƠNG THỨC VẼ ---
    public void draw(Graphics2D g2) {
        // 1. Vẽ nền mờ che toàn bộ cửa sổ
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        // 2. Vẽ tiêu đề (Sử dụng FontMetrics để căn giữa)
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 60));
        String title = "CHESS GAME";
        FontMetrics fmTitle = g2.getFontMetrics();
        int titleWidth = fmTitle.stringWidth(title);
        // Tính toán vị trí X để căn giữa TOÀN BỘ cửa sổ
        int titleX = (WINDOW_WIDTH - titleWidth) / 2;
        g2.drawString(title, titleX, START_Y - 50);

        // 3. Vẽ các nút
        g2.setFont(new Font("Arial", Font.PLAIN, 24));

        drawButton(g2, playButton, "PLAY NEW GAME", Color.GREEN.darker());
        drawButton(g2, loadButton, "LOAD / CONTINUE", Color.YELLOW.darker());
        drawButton(g2, settingsButton, "SETTINGS", Color.BLUE.darker());
        drawButton(g2, quitButton, "EXIT", Color.RED.darker());
    }

    private void drawButton(Graphics2D g2, Rectangle rect, String text, Color color) {
        g2.setColor(color);
        g2.fill(rect);

        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.draw(rect);

        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        int textX = rect.x + (rect.width - fm.stringWidth(text)) / 2;
        int textY = rect.y + ((rect.height - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(text, textX, textY);
    }
}