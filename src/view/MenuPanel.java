package view;

import controller.GameController;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class MenuPanel extends JPanel {
    private static final int LOGIC_H = 600;
    private static final int BTN_W = 300;
    private static final int BTN_H = 50;

    private final GameController controller;
    private BufferedImage backgroundImage;
    private BufferedImage[] slotThumbnails = new BufferedImage[4];

    private boolean isSettingsMode = false;
    private boolean isLoadMode = false;
    private int hoveredIndex = -1;

    private JSlider bgmSlider, sfxSlider;

    // Tọa độ Y cố định cho các thành phần logic
    private final int playY = 180, loadY = 240, settY = 300, exitY = 360, backY = 500;
    private final int[] slotY = {160, 240, 320, 400};

    public MenuPanel(GameController gc, JFrame frame) {
        this.controller = gc;
        setLayout(null);
        try { backgroundImage = ImageIO.read(new File("res/bg/menu_bg.png")); } catch (Exception e) {}

        initSliders();

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                double s = (double)getHeight() / LOGIC_H;
                int lx = (int)(e.getX() / s);
                int ly = (int)(e.getY() / s);
                int btnX = getCenteredBtnX();

                if (isLoadMode) {
                    for (int i = 0; i < 4; i++) {
                        if (lx >= btnX && lx <= btnX + BTN_W && ly >= slotY[i] && ly <= slotY[i] + 70) {
                            controller.loadGame(i + 1);
                            return;
                        }
                    }
                    if (isInside(lx, ly, btnX, backY)) isLoadMode = false;
                } else if (isSettingsMode) {
                    if (isInside(lx, ly, btnX, backY)) isSettingsMode = false;
                } else {
                    if (isInside(lx, ly, btnX, playY)) controller.startNewGame();
                    else if (isInside(lx, ly, btnX, loadY)) { loadThumbnails(); isLoadMode = true; }
                    else if (isInside(lx, ly, btnX, settY)) isSettingsMode = true;
                    else if (isInside(lx, ly, btnX, exitY)) System.exit(0);
                }
                repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                double s = (double)getHeight() / LOGIC_H;
                int lx = (int)(e.getX() / s);
                int ly = (int)(e.getY() / s);
                int btnX = getCenteredBtnX();

                int prevHover = hoveredIndex;
                hoveredIndex = -1;

                if (isLoadMode) {
                    for (int i = 0; i < 4; i++) {
                        if (lx >= btnX && lx <= btnX + BTN_W && ly >= slotY[i] && ly <= slotY[i] + 70) hoveredIndex = i;
                    }
                    if (isInside(lx, ly, btnX, backY)) hoveredIndex = 4;
                } else if (isSettingsMode) {
                    if (isInside(lx, ly, btnX, backY)) hoveredIndex = 4;
                } else {
                    if (isInside(lx, ly, btnX, playY)) hoveredIndex = 0;
                    else if (isInside(lx, ly, btnX, loadY)) hoveredIndex = 1;
                    else if (isInside(lx, ly, btnX, settY)) hoveredIndex = 2;
                    else if (isInside(lx, ly, btnX, exitY)) hoveredIndex = 3;
                }

                if (prevHover != hoveredIndex) {
                    setCursor(new Cursor(hoveredIndex != -1 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                    repaint();
                }
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    private void initSliders() {
        bgmSlider = new JSlider(0, 100, controller.getAudioManager().getBGMVolumeAsInt());
        sfxSlider = new JSlider(0, 100, controller.getAudioManager().getSFXVolumeAsInt());

        bgmSlider.setOpaque(false); sfxSlider.setOpaque(false);
        bgmSlider.setFocusable(false); sfxSlider.setFocusable(false);

        bgmSlider.addChangeListener(e -> controller.getAudioManager().setBGMVolumeFromSlider(bgmSlider.getValue()));
        sfxSlider.addChangeListener(e -> controller.getAudioManager().setSFXVolumeFromSlider(sfxSlider.getValue()));

        this.add(bgmSlider); this.add(sfxSlider);
    }

    private int getCenteredBtnX() {
        double s = (double)getHeight() / LOGIC_H;
        return (int)(((getWidth() / s) / 2) - (BTN_W / 2));
    }

    private boolean isInside(int lx, int ly, int bx, int by) {
        return lx >= bx && lx <= bx + BTN_W && ly >= by && ly <= by + BTN_H;
    }

    private void loadThumbnails() {
        for (int i = 0; i < 4; i++) slotThumbnails[i] = controller.getSlotThumbnail(i + 1);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Vẽ nền bao phủ kích thước thực
        if (backgroundImage != null) g2.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null);
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, getWidth(), getHeight());

        // 2. Tính Scale
        double s = (double)getHeight() / LOGIC_H;
        int centerX = (int)((getWidth() / s) / 2);
        int btnX = centerX - (BTN_W / 2);

        // 3. Cập nhật vị trí Slider (Tọa độ vật lý)
        if (isSettingsMode) {
            bgmSlider.setBounds((int)((centerX + 10) * s), (int)(305 * s), (int)(150 * s), (int)(30 * s));
            sfxSlider.setBounds((int)((centerX + 10) * s), (int)(385 * s), (int)(150 * s), (int)(30 * s));
            bgmSlider.setVisible(true); sfxSlider.setVisible(true);
        } else {
            bgmSlider.setVisible(false); sfxSlider.setVisible(false);
        }

        g2.scale(s, s);

        if (isLoadMode) {
            drawLoadUI(g2, centerX, btnX);
        } else if (isSettingsMode) {
            drawSettingsUI(g2, centerX, btnX);
        } else {
            drawMainUI(g2, centerX, btnX);
        }
    }

    private void drawMainUI(Graphics2D g2, int cx, int bx) {
        drawTitle(g2, "Chess Gemini", cx);
        drawStyledBtn(g2, bx, playY, "New Game", new Color(46, 204, 113), 0);
        drawStyledBtn(g2, bx, loadY, "Load Game", new Color(52, 152, 219), 1);
        drawStyledBtn(g2, bx, settY, "Settings", new Color(149, 165, 166), 2);
        drawStyledBtn(g2, bx, exitY, "Exit", new Color(231, 76, 60), 3);
    }

    private void drawLoadUI(Graphics2D g2, int cx, int bx) {
        drawTitle(g2, "SELECT SLOT", cx);
        for (int i = 0; i < 4; i++) {
            boolean h = (hoveredIndex == i);
            g2.setColor(h ? new Color(80, 80, 80) : new Color(45, 45, 45));
            g2.fillRect(bx, slotY[i], BTN_W, 70);
            g2.setColor(Color.WHITE); g2.drawRect(bx, slotY[i], BTN_W, 70);
            if (slotThumbnails[i] != null) g2.drawImage(slotThumbnails[i], bx + 5, slotY[i] + 5, 90, 60, null);
            g2.setFont(new Font("Arial", Font.BOLD, 16));
            g2.drawString("SLOT " + (i+1), bx + 105, slotY[i] + 25);
            g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
            g2.drawString(controller.getSlotMetadata(i+1), bx + 105, slotY[i] + 55);
        }
        drawStyledBtn(g2, bx, backY, "BACK", Color.GRAY, 4);
    }

    private void drawSettingsUI(Graphics2D g2, int cx, int bx) {
        drawTitle(g2, "SETTINGS", cx);
        g2.setColor(Color.WHITE); g2.setFont(new Font("Arial", Font.PLAIN, 20));
        g2.drawString("Music Volume", cx - 140, 325);
        g2.drawString("SFX Volume", cx - 140, 405);
        drawStyledBtn(g2, bx, backY, "BACK", Color.GRAY, 4);
    }

    private void drawTitle(Graphics2D g2, String txt, int cx) {
        g2.setColor(Color.WHITE); g2.setFont(new Font("Arial", Font.BOLD, 50));
        g2.drawString(txt, cx - g2.getFontMetrics().stringWidth(txt)/2, 100);
    }

    private void drawStyledBtn(Graphics2D g2, int x, int y, String txt, Color c, int idx) {
        boolean h = (hoveredIndex == idx);
        g2.setColor(h ? c.brighter() : c); g2.fillRect(x, y, BTN_W, BTN_H);
        g2.setColor(Color.BLACK); g2.drawRect(x, y, BTN_W, BTN_H);
        g2.setColor(Color.WHITE); g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.drawString(txt, x + (BTN_W - g2.getFontMetrics().stringWidth(txt))/2, y + 32);
    }

    public void resetMenu() {
        this.isLoadMode = false;
        this.isSettingsMode = false;
        this.hoveredIndex = -1;
        repaint();
    }
}