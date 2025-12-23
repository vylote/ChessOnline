package view;

import controller.GameController;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

public class PausePanel extends JPanel {

    private final GameController controller;
    private BufferedImage backgroundSnapshot;
    private static final int TOTAL_SLOTS = 4;
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;

    private final Rectangle[] slotBackgroundRects = new Rectangle[TOTAL_SLOTS];
    private final Rectangle[] saveClickRects = new Rectangle[TOTAL_SLOTS];
    private final Rectangle[] loadClickRects = new Rectangle[TOTAL_SLOTS];

    private final String[] menuItems = {"Continue", "Game settings", "Exit to menu"};
    private final Rectangle[] menuRects = new Rectangle[menuItems.length];

    private JSlider volumeSlider;
    private boolean showSettings = false;
    private int hoveredSlotIndex = -1;
    private boolean isHoveringSave = false;
    private int hoveredMenuIndex = -1;

    public PausePanel(GameController gc, JFrame containingFrame) {
        this.controller = gc;
        setLayout(null); // Bắt buộc để dùng setBounds cho JSlider
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));

        initLayout();
        initVolumeSlider();

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) { handleMouseHover(e.getX(), e.getY()); }
            @Override
            public void mouseReleased(MouseEvent e) { handleMouseClick(e.getX(), e.getY()); }
        };
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    private void initVolumeSlider() {
        // Lấy giá trị hiện tại từ AudioManager của Controller
        volumeSlider = new JSlider(0, 100, controller.getAudioManager().getVolumeAsInt());

        // Căn chỉnh tọa độ: x=880 để nhường chỗ cho Label "Volume" bên trái
        volumeSlider.setBounds(880, 335, 200, 30);
        volumeSlider.setOpaque(false);
        volumeSlider.setFocusable(false);
        volumeSlider.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        volumeSlider.addChangeListener(e -> {
            controller.getAudioManager().setVolumeFromSlider(volumeSlider.getValue());
            repaint(); // Vẽ lại để cập nhật con số %
        });
        this.add(volumeSlider);
        volumeSlider.setVisible(false);
    }

    private void initLayout() {
        // Slots Trái (Giữ nguyên)
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            int y = 100 + i * 130;
            slotBackgroundRects[i] = new Rectangle(50, y, 600, 110);
            saveClickRects[i] = new Rectangle(150, y, 500, 55);
            loadClickRects[i] = new Rectangle(150, y + 55, 500, 55);
        }
        // Menu Phải (Giữ nguyên)
        for (int i = 0; i < menuItems.length; i++) {
            menuRects[i] = new Rectangle(850, 100 + i * 90, 300, 50);
        }
    }

    public void setBackgroundSnapshot(BufferedImage snapshot) { this.backgroundSnapshot = snapshot; }

    private void handleMouseHover(int x, int y) {
        hoveredSlotIndex = -1; hoveredMenuIndex = -1;

        // Hover Slots
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (saveClickRects[i].contains(x, y)) { hoveredSlotIndex = i; isHoveringSave = true; break; }
            if (loadClickRects[i].contains(x, y)) { hoveredSlotIndex = i; isHoveringSave = false; break; }
        }
        // Hover Menu
        for (int i = 0; i < menuItems.length; i++) {
            if (menuRects[i].contains(x, y)) { hoveredMenuIndex = i; break; }
        }

        // Pointer Cursor logic
        if (hoveredSlotIndex != -1 || hoveredMenuIndex != -1 || (showSettings && volumeSlider.getBounds().contains(x, y))) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
        repaint();
    }

    private void handleMouseClick(int x, int y) {
        if (showSettings) {
            // Nhấn lại "Game settings" hoặc vùng tương ứng để quay lại
            if (menuRects[1].contains(x, y)) {
                showSettings = false;
                volumeSlider.setVisible(false);
            }
            return;
        }

        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (saveClickRects[i].contains(x, y)) { controller.saveGame(i + 1); repaint(); return; }
            if (loadClickRects[i].contains(x, y)) { controller.loadGame(i + 1); return; }
        }

        if (menuRects[0].contains(x, y)) controller.resumeGame();
        else if (menuRects[1].contains(x, y)) {
            showSettings = true;
            volumeSlider.setVisible(true);
        }
        else if (menuRects[2].contains(x, y)) controller.exitToMenu();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (backgroundSnapshot != null) g2.drawImage(backgroundSnapshot, 0, 0, null);

        // Overlay mờ
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        // Vẽ danh sách Slot bên trái
        for (int i = 0; i < TOTAL_SLOTS; i++) drawSlot(g2, i);

        if (showSettings) {
            drawSettingsUI(g2);
        } else {
            drawRightMenu(g2);
        }
    }

    private void drawSettingsUI(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 30));
        g2.drawString("SETTINGS", 850, 120);

        // Bố cục: Label — Slider — %
        g2.setFont(new Font("Arial", Font.PLAIN, 20));
        g2.setColor(Color.WHITE);
        g2.drawString("Volume", 780, 357); // Nhãn bên trái slider

        // Con số % bên phải slider
        g2.setColor(Color.YELLOW);
        g2.drawString(volumeSlider.getValue() + "%", 1100, 357);

        // Nút Back (vẽ đè lên vị trí Game Settings cũ để người dùng dễ thoát)
        g2.setColor(hoveredMenuIndex == 1 ? Color.YELLOW : Color.WHITE);
        g2.drawString("> Back", 850, 190 + 50);
    }

    private void drawSlot(Graphics2D g2, int index) {
        Rectangle bgRect = slotBackgroundRects[index];
        String metadata = controller.getSlotMetadata(index + 1);
        boolean hasData = !metadata.equals("Empty Slot");

        // Vẽ khung nền Slot
        g2.setColor(new Color(50, 50, 50, 200));
        g2.fillRect(bgRect.x, bgRect.y, bgRect.width, bgRect.height);

        // Highlight khi Hover
        if (hoveredSlotIndex == index) {
            g2.setColor(new Color(100, 100, 100, 150));
            Rectangle hRect = isHoveringSave ? saveClickRects[index] : loadClickRects[index];
            if (isHoveringSave || hasData) g2.fillRect(hRect.x, hRect.y, hRect.width, hRect.height);
        }

        // Vẽ số thứ tự Slot
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 40));
        g2.drawString(String.valueOf(index + 1), bgRect.x + 35, bgRect.y + 70);

        // Vẽ chữ Save/Load
        g2.setFont(new Font("Arial", Font.PLAIN, 18));
        g2.setColor(hoveredSlotIndex == index && isHoveringSave ? Color.YELLOW : Color.WHITE);
        g2.drawString("Save state", bgRect.x + 120, bgRect.y + 35);

        if (hasData) {
            g2.setColor(hoveredSlotIndex == index && !isHoveringSave ? Color.YELLOW : Color.WHITE);
            g2.drawString("Load state", bgRect.x + 120, bgRect.y + 90);

            g2.setFont(new Font("Monospaced", Font.PLAIN, 16));
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawString(metadata, bgRect.x + 230, bgRect.y + 90);
        }
    }

    private void drawRightMenu(Graphics2D g2) {
        g2.setFont(new Font("Arial", Font.PLAIN, 28));
        for (int i = 0; i < menuItems.length; i++) {
            g2.setColor(hoveredMenuIndex == i ? Color.YELLOW : Color.WHITE);
            g2.drawString((hoveredMenuIndex == i ? "> " : "") + menuItems[i], 850, 140 + i * 90);
        }
    }
}