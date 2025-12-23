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
    private boolean showLoadSlots = false; // Chuyển đổi giữa Menu chính và chọn Slot

    // --- BIẾN HOVER ---
    private int hoveredMainButton = -1; // 0: Play, 1: Load, 2: Settings, 3: Quit
    private int hoveredSlot = -1;       // 0-3 cho 4 slot
    private boolean hoveredBack = false;

    // --- HẰNG SỐ VỊ TRÍ ---
    private final Rectangle playButton;
    private final Rectangle loadMenuButton;
    private final Rectangle settingsButton;
    private final Rectangle quitButton;
    private final Rectangle[] slotButtons = new Rectangle[4];
    private final Rectangle backButton;

    private static final int BTN_W = 300;
    private static final int BTN_H = 60;

    public MenuPanel(GameController gc, JFrame containingFrame) {
        this.controller = gc;
        this.containingFrame = containingFrame;

        try {
            backgroundImage = ImageIO.read(new File("res/bg/menu_bg.png"));
        } catch (IOException e) {
            System.err.println("Lỗi: Không tải được ảnh nền.");
        }

        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));

        // 1. Khởi tạo nút Menu chính
        int startY = 320;
        playButton = new Rectangle((WINDOW_WIDTH - BTN_W) / 2, startY, BTN_W, BTN_H);
        loadMenuButton = new Rectangle((WINDOW_WIDTH - BTN_W) / 2, startY + 85, BTN_W, BTN_H);
        settingsButton = new Rectangle((WINDOW_WIDTH - BTN_W) / 2, startY + 170, BTN_W, BTN_H);
        quitButton = new Rectangle((WINDOW_WIDTH - BTN_W) / 2, startY + 255, BTN_W, BTN_H);

        // 2. Khởi tạo 4 Slot Load
        for (int i = 0; i < 4; i++) {
            slotButtons[i] = new Rectangle((WINDOW_WIDTH - 500) / 2, 250 + (i * 90), 500, 80);
        }
        backButton = new Rectangle((WINDOW_WIDTH - BTN_W) / 2, 650, BTN_W, BTN_H);

        // --- LẮNG NGHE CHUỘT ---
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

    // Giữ nguyên các khai báo Button và Rectangles như code bạn đã gửi
// Chỉ cần thay đổi logic trong handleMouseClick:

    private void handleMouseClick(int x, int y) {
        if (!showLoadSlots) {
            if (playButton.contains(x, y)) controller.startNewGame();
            else if (loadMenuButton.contains(x, y)) showLoadSlots = true;
            else if (settingsButton.contains(x, y)) { /* Mở settings */ }
            else if (quitButton.contains(x, y)) System.exit(0);
        } else {
            // TẠI ĐÂY: Chỉ có Load, không có Save
            for (int i = 0; i < 4; i++) {
                if (slotButtons[i].contains(x, y)) {
                    controller.loadGame(i + 1);
                    return;
                }
            }
            if (backButton.contains(x, y)) showLoadSlots = false;
        }
        repaint();
    }

    private void handleMouseHover(int x, int y) {
        hoveredMainButton = -1; hoveredSlot = -1; hoveredBack = false;

        if (!showLoadSlots) {
            if (playButton.contains(x, y)) hoveredMainButton = 0;
            else if (loadMenuButton.contains(x, y)) hoveredMainButton = 1;
            else if (settingsButton.contains(x, y)) hoveredMainButton = 2;
            else if (quitButton.contains(x, y)) hoveredMainButton = 3;
        } else {
            for (int i = 0; i < 4; i++) if (slotButtons[i].contains(x, y)) hoveredSlot = i;
            if (backButton.contains(x, y)) hoveredBack = true;
        }

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

        // Vẽ nền & Overlay
        if (backgroundImage != null) g2.drawImage(backgroundImage, 0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, null);
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

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

        // Hiển thị ngày giờ lưu
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
        int tx = rect.x + (rect.width - fm.stringWidth(text)) / 2;
        int ty = rect.y + (rect.height + fm.getAscent()) / 2 - 5;
        g2.drawString(text, tx, ty);
    }
}