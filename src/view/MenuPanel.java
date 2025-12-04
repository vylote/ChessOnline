package view;

import controller.GameController;
import model.GameState;
import model.State;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// GIẢ ĐỊNH: Lớp MenuPanel phải mở rộng JPanel (hoặc JComponent) để có thể gắn Listener
public class MenuPanel extends JPanel { // *CẦN THÊM extends JPanel*

    // Kích thước cửa sổ (Lấy từ GamePanel)
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;

    private final GameController controller;
    private final JFrame containingFrame;
    private final Rectangle playButton;
    private final Rectangle loadButton;
    private final Rectangle settingsButton;
    private final Rectangle quitButton;

    // --- HẰNG SỐ VẼ ---
    private static final int BUTTON_WIDTH = 250;
    private static final int BUTTON_HEIGHT = 60;
    private static final int BUTTON_SPACING = 20;

    // Tính toán CĂN GIỮA TOÀN BỘ
    private static final int CENTER_X = (WINDOW_WIDTH - BUTTON_WIDTH) / 2;
    private static final int TOTAL_HEIGHT = 4 * BUTTON_HEIGHT + 3 * BUTTON_SPACING;
    private static final int START_Y = (WINDOW_HEIGHT - TOTAL_HEIGHT) / 2;


    // --- CONSTRUCTOR (Đã sửa lỗi biên dịch) ---
    public MenuPanel(GameController gc, JFrame containingFrame) {
        this.controller = gc;
        this.containingFrame = containingFrame;

        // Thiết lập kích thước cho Panel này
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setLayout(null); // Giữ Layout đơn giản

        // Định vị các nút
        this.playButton = new Rectangle(CENTER_X, START_Y, BUTTON_WIDTH, BUTTON_HEIGHT);
        this.loadButton = new Rectangle(CENTER_X, START_Y + BUTTON_HEIGHT + BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT);
        this.settingsButton = new Rectangle(CENTER_X, START_Y + 2 * (BUTTON_HEIGHT + BUTTON_SPACING), BUTTON_WIDTH, BUTTON_HEIGHT);
        this.quitButton = new Rectangle(CENTER_X, START_Y + 3 * (BUTTON_HEIGHT + BUTTON_SPACING), BUTTON_WIDTH, BUTTON_HEIGHT);

        // --- LOGIC XỬ LÝ CLICK (Sửa lỗi gp -> this) ---
        this.addMouseListener(new MouseAdapter() { // Gắn MouseListener vào chính MenuPanel
            @Override
            public void mouseReleased(MouseEvent e) {
                // Kiểm tra trạng thái Menu để đảm bảo không xử lý nhầm (dù Frame này chỉ xuất hiện ở trạng thái Menu)
                if (GameState.currentState == State.MENU) {
                    int x = e.getX();
                    int y = e.getY();

                    if (playButton.contains(x, y)) {
                        controller.startNewGame(); // Controller sẽ đóng Frame này
                    } else if (loadButton.contains(x, y)) {
                        System.out.println("Load Game clicked (TBD)");
                    } else if (settingsButton.contains(x, y)) {
                        System.out.println("Settings clicked (TBD)");
                    } else if (quitButton.contains(x, y)) {
                        containingFrame.dispose(); // Đóng Frame chứa Menu
                        System.exit(0);
                    }
                }
            }
        });
    }

    // --- PHƯƠNG THỨC VẼ (paintComponent cần được gọi để vẽ) ---
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        draw(g2); // Gọi hàm draw chính
    }

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