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
    private BufferedImage[] slotThumbnails = new BufferedImage[TOTAL_SLOTS];
    private final Rectangle[] saveClickRects = new Rectangle[TOTAL_SLOTS];
    private final Rectangle[] loadClickRects = new Rectangle[TOTAL_SLOTS];

    private final String[] menuItems = {"Continue", "Game settings", "Exit to menu"};
    private final Rectangle[] menuRects = new Rectangle[menuItems.length];

    // --- HAI THANH TRƯỢT ĐỘC LẬP ---
    private JSlider bgmSlider;
    private JSlider sfxSlider;

    private boolean showSettings = false;
    private int hoveredSlotIndex = -1;
    private boolean isHoveringSave = false;
    private int hoveredMenuIndex = -1;

    public PausePanel(GameController gc, JFrame containingFrame) {
        this.controller = gc;
        setLayout(null);
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));

        initLayout();
        initVolumeSliders();

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) { handleMouseHover(e.getX(), e.getY()); }
            @Override
            public void mouseReleased(MouseEvent e) { handleMouseClick(e.getX(), e.getY()); }
        };
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    private void initVolumeSliders() {
        // BGM Slider
        bgmSlider = new JSlider(0, 100, controller.getAudioManager().getBGMVolumeAsInt());
        bgmSlider.setBounds(880, 300, 200, 30);
        setupSliderStyle(bgmSlider);
        bgmSlider.addChangeListener(e -> {
            controller.getAudioManager().setBGMVolumeFromSlider(bgmSlider.getValue());
            repaint();
        });

        // SFX Slider
        sfxSlider = new JSlider(0, 100, controller.getAudioManager().getSFXVolumeAsInt());
        sfxSlider.setBounds(880, 380, 200, 30);
        setupSliderStyle(sfxSlider);
        sfxSlider.addChangeListener(e -> {
            controller.getAudioManager().setSFXVolumeFromSlider(sfxSlider.getValue());
            repaint();
        });

        this.add(bgmSlider);
        this.add(sfxSlider);
    }

    private void setupSliderStyle(JSlider slider) {
        slider.setOpaque(false);
        slider.setFocusable(false);
        slider.setVisible(false);
        slider.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void initLayout() {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            int y = 100 + i * 130;
            slotBackgroundRects[i] = new Rectangle(50, y, 600, 110);
            saveClickRects[i] = new Rectangle(150, y, 500, 55);
            loadClickRects[i] = new Rectangle(150, y + 55, 500, 55);
        }
        for (int i = 0; i < menuItems.length; i++) {
            menuRects[i] = new Rectangle(850, 100 + i * 90, 300, 50);
        }
    }

    public void setBackgroundSnapshot(BufferedImage snapshot) { this.backgroundSnapshot = snapshot; }

    private void handleMouseHover(int x, int y) {
        hoveredSlotIndex = -1; hoveredMenuIndex = -1;

        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (saveClickRects[i].contains(x, y)) { hoveredSlotIndex = i; isHoveringSave = true; break; }
            if (loadClickRects[i].contains(x, y)) { hoveredSlotIndex = i; isHoveringSave = false; break; }
        }
        for (int i = 0; i < menuItems.length; i++) {
            if (menuRects[i].contains(x, y)) { hoveredMenuIndex = i; break; }
        }

        boolean hoveringSlider = (showSettings && (bgmSlider.getBounds().contains(x, y) || sfxSlider.getBounds().contains(x, y)));
        if (hoveredSlotIndex != -1 || hoveredMenuIndex != -1 || hoveringSlider) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            setCursor(Cursor.getDefaultCursor());
        }

        repaint();
    }

    public void loadAllThumbnails() {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            slotThumbnails[i] = controller.getSlotThumbnail(i + 1);
        }
    }

    private void handleMouseClick(int x, int y) {
        if (showSettings) {
            if (menuRects[1].contains(x, y)) {
                showSettings = false;
                bgmSlider.setVisible(false);
                sfxSlider.setVisible(false);
            }
            return;
        }

        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (saveClickRects[i].contains(x, y)) {
                controller.saveGame(i + 1, backgroundSnapshot);
                slotThumbnails[i] = backgroundSnapshot;
                repaint();
                return;
            }
            if (loadClickRects[i].contains(x, y)) { controller.loadGame(i + 1); return; }
        }

        if (menuRects[0].contains(x, y)) controller.resumeGame();
        else if (menuRects[1].contains(x, y)) {
            showSettings = true;
            bgmSlider.setVisible(true);
            sfxSlider.setVisible(true);
        }
        else if (menuRects[2].contains(x, y)) controller.exitToMenu();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (backgroundSnapshot != null) g2.drawImage(backgroundSnapshot, 0, 0, null);
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        for (int i = 0; i < TOTAL_SLOTS; i++) drawSlot(g2, i);

        if (showSettings) drawSettingsUI(g2);
        else drawRightMenu(g2);
    }

    private void drawSettingsUI(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 30));
        g2.drawString("SETTINGS", 850, 120);

        g2.setFont(new Font("Arial", Font.PLAIN, 18));

        // Music Row
        g2.setColor(Color.WHITE);
        g2.drawString("Music", 780, 322);
        g2.setColor(Color.YELLOW);
        g2.drawString(bgmSlider.getValue() + "%", 1100, 322);

        // SFX Row
        g2.setColor(Color.WHITE);
        g2.drawString("SFX", 780, 402);
        g2.setColor(Color.YELLOW);
        g2.drawString(sfxSlider.getValue() + "%", 1100, 402);

        g2.setColor(hoveredMenuIndex == 1 ? Color.YELLOW : Color.WHITE);
        g2.drawString("> Back", 850, 240);
    }

    private void drawSlot(Graphics2D g2, int index) {
        Rectangle bgRect = slotBackgroundRects[index];
        String metadata = controller.getSlotMetadata(index + 1);
        boolean hasData = !metadata.equals("Empty Slot");

        // 1. Vẽ khung nền slot
        g2.setColor(new Color(50, 50, 50, 200));
        g2.fillRect(bgRect.x, bgRect.y, bgRect.width, bgRect.height);

        // 2. VẼ THUMBNAIL HOẶC SỐ (Chỉ vẽ 1 trong 2)
        int thumbX = bgRect.x + 5;
        int thumbY = bgRect.y + 5;
        int thumbW = 140; // Tăng chiều rộng một chút để tỉ lệ ảnh 1200x800 trông cân đối hơn
        int thumbH = 100;

        if (slotThumbnails[index] != null) {
            // Vẽ ảnh chụp trận đấu đã lưu
            g2.drawImage(slotThumbnails[index], thumbX, thumbY, thumbW, thumbH, null);
            // Vẽ viền mỏng quanh ảnh
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1));
            g2.drawRect(thumbX, thumbY, thumbW, thumbH);
        } else {
            // NẾU CHƯA CÓ ẢNH: Vẽ con số mặc định mờ hơn ở nền
            g2.setColor(new Color(255, 255, 255, 50));
            g2.setFont(new Font("Arial", Font.BOLD, 50));
            g2.drawString(String.valueOf(index + 1), bgRect.x + 45, bgRect.y + 70);
        }

        // 3. Highlight vùng khi Hover (Save/Load)
        if (hoveredSlotIndex == index) {
            g2.setColor(new Color(255, 255, 255, 30));
            Rectangle hRect = isHoveringSave ? saveClickRects[index] : loadClickRects[index];
            // Chỉ cho phép highlight Load nếu slot có dữ liệu
            if (isHoveringSave || hasData) {
                g2.fillRect(hRect.x, hRect.y, hRect.width, hRect.height);
            }
        }

        // 4. Vẽ text hướng dẫn (Save / Load)
        g2.setFont(new Font("Arial", Font.PLAIN, 18));

        // Vẽ "Save state"
        g2.setColor(hoveredSlotIndex == index && isHoveringSave ? Color.YELLOW : Color.WHITE);
        g2.drawString("Save state", bgRect.x + 160, bgRect.y + 35);

        // Vẽ "Load state" + Metadata
        if (hasData) {
            g2.setColor(hoveredSlotIndex == index && !isHoveringSave ? Color.YELLOW : Color.WHITE);
            g2.drawString("Load state", bgRect.x + 160, bgRect.y + 90);

            g2.setFont(new Font("Monospaced", Font.PLAIN, 14));
            g2.setColor(new Color(180, 180, 180));
            g2.drawString(metadata, bgRect.x + 280, bgRect.y + 90);
        } else {
            g2.setColor(Color.DARK_GRAY);
            g2.drawString("Empty Slot", bgRect.x + 160, bgRect.y + 90);
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