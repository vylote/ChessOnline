package view;

import controller.GameController;
import model.GameState;
import model.State;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage; // Cần import cho hình ảnh
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class MenuPanel extends JPanel {

    // Kích thước cửa sổ (Lấy từ GamePanel)
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;

    private final GameController controller;
    private final JFrame containingFrame;
    private final Rectangle playButton;
    private final Rectangle loadButton;
    private final Rectangle settingsButton;
    private final Rectangle quitButton;

    // NEW: Biến để lưu trữ hình ảnh nền
    private BufferedImage backgroundImage;

    // --- HẰNG SỐ VẼ ---
    private static final int BUTTON_WIDTH = 250;
    private static final int BUTTON_HEIGHT = 60;
    private static final int BUTTON_SPACING = 20;

    // Tính toán CĂN GIỮA TOÀN BỘ
    private static final int CENTER_X = (WINDOW_WIDTH - BUTTON_WIDTH) / 2;
    private static final int TOTAL_HEIGHT = 4 * BUTTON_HEIGHT + 3 * BUTTON_SPACING;
    private static final int START_Y = (WINDOW_HEIGHT - TOTAL_HEIGHT) / 2;

    // --- CONSTRUCTOR ---
    public MenuPanel(GameController gc, JFrame containingFrame) {
        this.controller = gc;
        this.containingFrame = containingFrame;

        // NEW: Tải hình ảnh nền
        try {
            // Thay đổi "res/menu_background.jpg" bằng đường dẫn file ảnh nền thực tế của bạn
            backgroundImage = ImageIO.read(new File("res/bg/menu_bg.png"));
        } catch (IOException e) {
            System.err.println("Lỗi: Không thể tải hình ảnh nền cho Menu.");
            e.printStackTrace();
        }

        // Thiết lập kích thước cho Panel này
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setLayout(null);

        // Định vị các nút
        this.playButton = new Rectangle(CENTER_X, START_Y, BUTTON_WIDTH, BUTTON_HEIGHT);
        this.loadButton = new Rectangle(CENTER_X, START_Y + BUTTON_HEIGHT + BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT);
        this.settingsButton = new Rectangle(CENTER_X, START_Y + 2 * (BUTTON_HEIGHT + BUTTON_SPACING), BUTTON_WIDTH, BUTTON_HEIGHT);
        this.quitButton = new Rectangle(CENTER_X, START_Y + 3 * (BUTTON_HEIGHT + BUTTON_SPACING), BUTTON_WIDTH, BUTTON_HEIGHT);

        // --- LOGIC XỬ LÝ CLICK ---
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (GameState.currentState == State.MENU) {
                    int x = e.getX();
                    int y = e.getY();

                    if (playButton.contains(x, y)) {
                        controller.startNewGame();
                    } else if (loadButton.contains(x, y)) {
                        controller.loadGame();
                    } else if (settingsButton.contains(x, y)) {
                        System.out.println("Settings clicked (TBD)");
                    } else if (quitButton.contains(x, y)) {
                        containingFrame.dispose();
                        System.exit(0);
                    }
                }
            }
        });
    }

    // --- PHƯƠNG THỨC VẼ ---
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        draw(g2);
    }

    public void draw(Graphics2D g2) {

        // BƯỚC 1: VẼ HÌNH ẢNH NỀN
        if (backgroundImage != null) {
            g2.drawImage(backgroundImage, 0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, null);
        } else {
            // Nếu ảnh không tải được, vẽ nền đen như trước
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        }

        // BƯỚC 2: VẼ LỚP PHỦ MỜ (Overlay)
        // Điều này giúp tiêu đề và nút dễ đọc hơn trên nền ảnh
        g2.setColor(new Color(0, 0, 0, 100)); // Màu đen, độ trong suốt A=100
        g2.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        // 3. Vẽ tiêu đề (Sử dụng FontMetrics để căn giữa)
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 60));
        String title = "CHESS GAME";
        FontMetrics fmTitle = g2.getFontMetrics();
        int titleWidth = fmTitle.stringWidth(title);
        int titleX = (WINDOW_WIDTH - titleWidth) / 2;
        g2.drawString(title, titleX, START_Y - 50);

        // 4. Vẽ các nút
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