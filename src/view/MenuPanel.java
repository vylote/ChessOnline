package view;

import controller.GameController;
import model.GameState;
import model.State;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class MenuPanel extends JPanel {

    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;

    private final GameController controller;
    private final JFrame containingFrame;
    private BufferedImage backgroundImage;

    // --- TRẠNG THÁI MENU ---
    private boolean showLoadSlots = false;
    private boolean showSettings = false;

    // --- BIẾN HOVER ---
    private int hoveredMainButton = -1;
    private int hoveredSlot = -1;
    private boolean hoveredBack = false;

    // --- COMPONENT & LAYOUT ---
    private JSlider volumeSlider; // Biến toàn cục
    private final Rectangle playButton, loadMenuButton, settingsButton, quitButton, backButton;
    private final Rectangle[] slotButtons = new Rectangle[4];

    private static final int BTN_W = 300;
    private static final int BTN_H = 60;

    public MenuPanel(GameController gc, JFrame containingFrame) {
        this.controller = gc;
        this.containingFrame = containingFrame;

        setLayout(null); // Bắt buộc để dùng setBounds cho JSlider
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));

        try {
            backgroundImage = ImageIO.read(new File("res/bg/menu_bg.png"));
        } catch (IOException e) {
            System.err.println("Lỗi: Không tải được ảnh nền.");
        }

        // 1. Khởi tạo Slider (Quan trọng: Gọi hàm khởi tạo ở đây)
        initVolumeSlider();

        // 2. Khởi tạo tọa độ nút Menu chính
        int startY = 320;
        playButton = new Rectangle((WINDOW_WIDTH - BTN_W) / 2, startY, BTN_W, BTN_H);
        loadMenuButton = new Rectangle((WINDOW_WIDTH - BTN_W) / 2, startY + 85, BTN_W, BTN_H);
        settingsButton = new Rectangle((WINDOW_WIDTH - BTN_W) / 2, startY + 170, BTN_W, BTN_H);
        quitButton = new Rectangle((WINDOW_WIDTH - BTN_W) / 2, startY + 255, BTN_W, BTN_H);

        // 3. Khởi tạo 4 Slot Load
        for (int i = 0; i < 4; i++) {
            slotButtons[i] = new Rectangle((WINDOW_WIDTH - 500) / 2, 250 + (i * 90), 500, 80);
        }
        backButton = new Rectangle((WINDOW_WIDTH - BTN_W) / 2, 650, BTN_W, BTN_H);

        // --- LISTENERS ---
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                handleMouseClick(e.getX(), e.getY());
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleMouseHover(e.getX(), e.getY());
            }
        });
    }

    private void initVolumeSlider() {
        // Lấy giá trị hiện tại từ AudioManager
        volumeSlider = new JSlider(0, 100, controller.getAudioManager().getVolumeAsInt());

        // Căn chỉnh Slider nằm giữa Label và Chỉ số %
        volumeSlider.setBounds(500, 400, 300, 30);
        volumeSlider.setOpaque(false);
        volumeSlider.setFocusable(false);
        volumeSlider.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        volumeSlider.addChangeListener(e -> {
            controller.getAudioManager().setVolumeFromSlider(volumeSlider.getValue());
            repaint(); // Vẽ lại để cập nhật con số % trên màn hình
        });

        this.add(volumeSlider);
        volumeSlider.setVisible(false); // Mặc định ẩn
    }

    private void handleMouseClick(int x, int y) {
        if (showSettings) {
            if (backButton.contains(x, y)) {
                showSettings = false;
                volumeSlider.setVisible(false);
            }
        } else if (showLoadSlots) {
            for (int i = 0; i < 4; i++) {
                if (slotButtons[i].contains(x, y)) {
                    controller.loadGame(i + 1);
                    return;
                }
            }
            if (backButton.contains(x, y)) showLoadSlots = false;
        } else {
            if (playButton.contains(x, y)) controller.startNewGame();
            else if (loadMenuButton.contains(x, y)) showLoadSlots = true;
            else if (settingsButton.contains(x, y)) {
                showSettings = true;
                volumeSlider.setVisible(true); // Hiện slider khi vào settings
            }
            else if (quitButton.contains(x, y)) System.exit(0);
        }
        repaint();
    }

    private void handleMouseHover(int x, int y) {
        hoveredMainButton = -1; hoveredSlot = -1; hoveredBack = false;

        if (showSettings || showLoadSlots) {
            if (backButton.contains(x, y)) hoveredBack = true;
        } else {
            if (playButton.contains(x, y)) hoveredMainButton = 0;
            else if (loadMenuButton.contains(x, y)) hoveredMainButton = 1;
            else if (settingsButton.contains(x, y)) hoveredMainButton = 2;
            else if (quitButton.contains(x, y)) hoveredMainButton = 3;
        }

        // Đổi con trỏ chuột
        if (hoveredMainButton != -1 || hoveredSlot != -1 || hoveredBack) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Vẽ nền
        if (backgroundImage != null) g2.drawImage(backgroundImage, 0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, null);
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        if (showSettings) {
            drawSettingsUI(g2);
        } else {
            // Vẽ Tiêu đề
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 70));
            String title = showLoadSlots ? "SELECT SAVE SLOT" : "CHESS GAME 2025";
            g2.drawString(title, (WINDOW_WIDTH - g2.getFontMetrics().stringWidth(title)) / 2, 180);

            if (!showLoadSlots) {
                drawButton(g2, playButton, "PLAY NEW GAME", new Color(40, 167, 69), hoveredMainButton == 0);
                drawButton(g2, loadMenuButton, "LOAD GAME", new Color(255, 193, 7), hoveredMainButton == 1);
                drawButton(g2, settingsButton, "SETTINGS", new Color(0, 123, 255), hoveredMainButton == 2);
                drawButton(g2, quitButton, "EXIT", new Color(220, 53, 69), hoveredMainButton == 3);
            } else {
                for (int i = 0; i < 4; i++) drawSlotButton(g2, i);
                drawButton(g2, backButton, "BACK", Color.GRAY, hoveredBack);
            }
        }
    }

    private void drawSettingsUI(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 60));
        g2.drawString("SETTINGS", (WINDOW_WIDTH - g2.getFontMetrics().stringWidth("SETTINGS")) / 2, 180);

        // Bố cục: Label - Slider (tự vẽ bởi Component) - %
        g2.setFont(new Font("Arial", Font.PLAIN, 25));
        g2.drawString("Music Volume", 320, 422);

        g2.setColor(Color.YELLOW);
        g2.drawString(volumeSlider.getValue() + "%", 820, 422);

        drawButton(g2, backButton, "BACK", Color.GRAY, hoveredBack);
    }

    private void drawSlotButton(Graphics2D g2, int index) {
        Rectangle r = slotButtons[index];
        boolean isH = (hoveredSlot == index);
        g2.setColor(isH ? new Color(80, 80, 80) : new Color(50, 50, 50));
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 15, 15);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(isH ? 3 : 1));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 15, 15);
        g2.setFont(new Font("Arial", Font.BOLD, 22));
        g2.drawString("SLOT " + (index + 1), r.x + 30, r.y + 35);

        g2.setFont(new Font("Monospaced", Font.PLAIN, 16));
        g2.setColor(new Color(200, 200, 200));
        g2.drawString(controller.getSlotMetadata(index + 1), r.x + 30, r.y + 60);
    }

    private void drawButton(Graphics2D g2, Rectangle rect, String text, Color color, boolean isH) {
        g2.setColor(isH ? color.brighter() : color);
        g2.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 10, 10);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(isH ? 3 : 1));
        g2.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 10, 10);
        g2.setFont(new Font("Arial", Font.BOLD, 22));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, rect.x + (rect.width - fm.stringWidth(text)) / 2, rect.y + (rect.height + fm.getAscent()) / 2 - 5);
    }
}