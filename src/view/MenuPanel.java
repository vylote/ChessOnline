package view;

import controller.GameController;
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
    private BufferedImage[] slotThumbnails = new BufferedImage[4];

    private boolean showLoadSlots = false;
    private boolean showSettings = false;

    private int hoveredMainButton = -1;
    private int hoveredSlot = -1;
    private boolean hoveredBack = false;

    // --- HAI THANH TRƯỢT ĐỘC LẬP ---
    private JSlider bgmSlider;
    private JSlider sfxSlider;

    private final Rectangle playButton, loadMenuButton, settingsButton, quitButton, backButton;
    private final Rectangle[] slotButtons = new Rectangle[4];

    private static final int BTN_W = 300;
    private static final int BTN_H = 60;

    public MenuPanel(GameController gc, JFrame containingFrame) {
        this.controller = gc;
        this.containingFrame = containingFrame;

        setLayout(null);
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));

        try {
            backgroundImage = ImageIO.read(new File("res/bg/menu_bg.png"));
        } catch (IOException e) {
            System.err.println("Lỗi: Không tải được ảnh nền.");
        }

        // 1. Khởi tạo 2 Sliders
        initVolumeSliders();

        // 2. Khởi tạo tọa độ nút
        int startY = 320;
        playButton = new Rectangle((WINDOW_WIDTH - BTN_W) / 2, startY, BTN_W, BTN_H);
        loadMenuButton = new Rectangle((WINDOW_WIDTH - BTN_W) / 2, startY + BTN_H, BTN_W, BTN_H);
        settingsButton = new Rectangle((WINDOW_WIDTH - BTN_W) / 2, startY + BTN_H*2, BTN_W, BTN_H);
        quitButton = new Rectangle((WINDOW_WIDTH - BTN_W) / 2, startY + BTN_H*3, BTN_W, BTN_H);

        for (int i = 0; i < 4; i++) {
            slotButtons[i] = new Rectangle((WINDOW_WIDTH - 500) / 2, 250 + (i * 90), 500, 80);
        }
        backButton = new Rectangle((WINDOW_WIDTH - BTN_W) / 2, 650, BTN_W, BTN_H);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) { handleMouseClick(e.getX(), e.getY()); }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) { handleMouseHover(e.getX(), e.getY()); }
        });
    }

    private void initVolumeSliders() {
        // BGM Slider
        bgmSlider = new JSlider(0, 100, controller.getAudioManager().getBGMVolumeAsInt());
        bgmSlider.setBounds(500, 360, 300, 30);
        setupSliderStyle(bgmSlider);
        bgmSlider.addChangeListener(e -> {
            controller.getAudioManager().setBGMVolumeFromSlider(bgmSlider.getValue());
            repaint();
        });

        // SFX Slider
        sfxSlider = new JSlider(0, 100, controller.getAudioManager().getSFXVolumeAsInt());
        sfxSlider.setBounds(500, 440, 300, 30);
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

    private void handleMouseClick(int x, int y) {
        if (showSettings) {
            if (backButton.contains(x, y)) {
                showSettings = false;
                bgmSlider.setVisible(false);
                sfxSlider.setVisible(false);
            }
        } else if (showLoadSlots) {
            for (int i = 0; i < 4; i++) {
                if (slotButtons[i].contains(x, y)) { controller.loadGame(i + 1); return; }
            }
            if (backButton.contains(x, y)) showLoadSlots = false;
        } else {
            if (playButton.contains(x, y)) controller.startNewGame();
            else if (loadMenuButton.contains(x, y)) {
                loadThumbnails();
                showLoadSlots = true;
            }
            else if (settingsButton.contains(x, y)) {
                showSettings = true;
                bgmSlider.setVisible(true);
                sfxSlider.setVisible(true);
            }
            else if (quitButton.contains(x, y)) System.exit(0);
        }
        repaint();
    }

    private void handleMouseHover(int x, int y) {
        hoveredMainButton = -1; hoveredSlot = -1; hoveredBack = false;


        if (showSettings || showLoadSlots) {
            for (int i = 0; i < 4; i++) {
                if (slotButtons[i].contains(x, y)) {
                    hoveredSlot = i;

                    break;
                }
            }
            if (backButton.contains(x, y)) { hoveredBack = true;}
        } else {
            if (playButton.contains(x, y)) { hoveredMainButton = 0;}
            else if (loadMenuButton.contains(x, y)) { hoveredMainButton = 1;}
            else if (settingsButton.contains(x, y)) { hoveredMainButton = 2;}
            else if (quitButton.contains(x, y)) { hoveredMainButton = 3;}
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

        if (backgroundImage != null) g2.drawImage(backgroundImage, 0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, null);
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        if (showSettings) {
            drawSettingsUI(g2);
        } else {
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 70));
            String title = showLoadSlots ? "SELECT SAVE SLOT" : "CHESS GAME 2025";
            g2.drawString(title, (WINDOW_WIDTH - g2.getFontMetrics().stringWidth(title)) / 2, 180);

            if (!showLoadSlots) {
                drawButton(g2, playButton, "PLAY NEW GAME", new Color(137, 154, 148, 255), hoveredMainButton == 0);
                drawButton(g2, loadMenuButton, "LOAD GAME", new Color(137, 154, 148, 255), hoveredMainButton == 1);
                drawButton(g2, settingsButton, "SETTINGS", new Color(137, 154, 148, 255), hoveredMainButton == 2);
                drawButton(g2, quitButton, "EXIT", new Color(137, 154, 148, 255), hoveredMainButton == 3);
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

        g2.setFont(new Font("Arial", Font.PLAIN, 25));

        // Music Row
        g2.setColor(Color.WHITE);
        g2.drawString("Music Volume", 320, 382);
        g2.setColor(Color.YELLOW);
        g2.drawString(bgmSlider.getValue() + "%", 820, 382);

        // SFX Row
        g2.setColor(Color.WHITE);
        g2.drawString("Sound FX", 320, 462);
        g2.setColor(Color.YELLOW);
        g2.drawString(sfxSlider.getValue() + "%", 820, 462);

        drawButton(g2, backButton, "BACK", Color.GRAY, hoveredBack);
    }

    private void drawSlotButton(Graphics2D g2, int index) {
        Rectangle r = slotButtons[index];
        boolean isH = (hoveredSlot == index);
        g2.setColor(isH ? new Color(80, 80, 80) : new Color(50, 50, 50));
        g2.fillRect(r.x, r.y, r.width, r.height);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(isH ? 3 : 1));
        g2.drawRect(r.x, r.y, r.width, r.height);

        // 2. THIẾT LẬP VỊ TRÍ ẢNH
        int imgW = 120; // Độ rộng ảnh thumbnail
        int imgH = r.height - 10;
        int imgX = r.x + 5;
        int imgY = r.y + 5;

        // 3. VẼ THUMBNAIL
        if (slotThumbnails[index] != null) {
            // Vẽ ảnh snapshot đã lưu từ file .png
            g2.drawImage(slotThumbnails[index], imgX, imgY, imgW, imgH, null);
        } else {
            g2.setColor(new Color(40, 40, 40));
            g2.fillRect(imgX, imgY, imgW, imgH);
            g2.setColor(Color.DARK_GRAY);
            g2.drawRect(imgX, imgY, imgW, imgH);
            g2.drawString("SLOT " + (index + 1), r.x + 30, r.y + 35);
        }

        int textX = imgX + imgW + 15;
        g2.setFont(new Font("Arial", Font.BOLD, 22));
        g2.setFont(new Font("Monospaced", Font.PLAIN, 16));
        g2.setColor(new Color(200, 200, 200));
        g2.drawString(controller.getSlotMetadata(index + 1), textX, r.y + 60);
    }

    private void drawButton(Graphics2D g2, Rectangle rect, String text, Color color, boolean isH) {
        g2.setColor(isH ? color.brighter() : color);
        g2.fillRect(rect.x, rect.y, rect.width, rect.height);
        g2.setColor(Color.GRAY);
        g2.setStroke(new BasicStroke(isH ? 3 : 1));
        g2.drawRect(rect.x, rect.y, rect.width, rect.height);
        g2.setFont(new Font("Arial", Font.BOLD, 22));
        g2.setColor(Color.BLACK);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, rect.x + (rect.width - fm.stringWidth(text)) / 2, rect.y + (rect.height + fm.getAscent()) / 2 - 5);
    }

    private void loadThumbnails() {
        for (int i = 0; i < 4; i++) {
            slotThumbnails[i] = controller.getSlotThumbnail(i + 1);
        }
    }
}