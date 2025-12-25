package view;

import controller.GameController;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class PausePanel extends JPanel {
    private static final int LOGIC_H = 600;
    private static final int SLOT_W = 400;

    private final GameController controller;
    private BufferedImage backgroundSnapshot;
    private BufferedImage[] slotThumbnails = new BufferedImage[4];

    private boolean showSettings = false;
    private int hoveredSlot = -1, hoveredMenu = -1;
    private boolean isHoveringSave = true;

    private JSlider bgmSlider, sfxSlider;
    private final int[] slotY = {100, 185, 270, 355};
    private final Rectangle[] menuBtns = {new Rectangle(0, 460, 200, 45), new Rectangle(0, 515, 200, 45)};

    public PausePanel(GameController gc, JFrame frame) {
        this.controller = gc;
        setLayout(null);
        initVolumeSliders();

        setupKeyBindings();

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                double s = (double)getHeight() / LOGIC_H;
                int lx = (int)(e.getX() / s), ly = (int)(e.getY() / s);
                int cx = (int)((getWidth()/s)/2);

                if (showSettings) {
                    if (isInside(lx, ly, cx - 100, 460, 200, 45)) showSettings = false;
                } else {
                    for (int i = 0; i < 4; i++) {
                        if (lx >= cx - 200 && lx <= cx + 200 && ly >= slotY[i] && ly <= slotY[i] + 75) {
                            if (ly < slotY[i] + 37) {
                                controller.saveGame(i + 1, backgroundSnapshot);
                                loadAllThumbnails();
                            } else {
                                controller.loadGame(i + 1);
                            }
                            return;
                        }
                    }
                    if (isInside(lx, ly, cx - 210, 460, 200, 45)) controller.resumeGame();
                    if (isInside(lx, ly, cx + 10, 460, 200, 45)) showSettings = true;
                    if (isInside(lx, ly, cx - 100, 515, 200, 45)) controller.exitToMenu();
                }
                repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                double s = (double)getHeight() / LOGIC_H;
                int lx = (int)(e.getX() / s), ly = (int)(e.getY() / s);
                int cx = (int)((getWidth()/s)/2);

                hoveredSlot = -1; hoveredMenu = -1;
                if (!showSettings) {
                    for (int i = 0; i < 4; i++) {
                        if (lx >= cx - 200 && lx <= cx + 200 && ly >= slotY[i] && ly <= slotY[i] + 75) {
                            hoveredSlot = i; isHoveringSave = (ly < slotY[i] + 37);
                        }
                    }
                }
                repaint();
            }
        };
        addMouseListener(ma); addMouseMotionListener(ma);
    }

    // --- LOGIC XỬ LÝ PHÍM ESC ---
    private void setupKeyBindings() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        // Gán phím ESCAPE với hành động "returnToMenu"
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "continueGame");

        am.put("continueGame", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Chỉ cho phép ESC thoát ra Menu khi đã Game Over
                controller.resumeGame();
            }
        });
    }

    private boolean isInside(int lx, int ly, int x, int y, int w, int h) {
        return lx >= x && lx <= x + w && ly >= y && ly <= y + h;
    }

    private void initVolumeSliders() {
        bgmSlider = new JSlider(0, 100, controller.getAudioManager().getBGMVolumeAsInt());
        sfxSlider = new JSlider(0, 100, controller.getAudioManager().getSFXVolumeAsInt());
        bgmSlider.setOpaque(false); sfxSlider.setOpaque(false);
        bgmSlider.addChangeListener(e -> controller.getAudioManager().setBGMVolumeFromSlider(bgmSlider.getValue()));
        sfxSlider.addChangeListener(e -> controller.getAudioManager().setSFXVolumeFromSlider(sfxSlider.getValue()));
        this.add(bgmSlider); this.add(sfxSlider);
    }

    public void setBackgroundSnapshot(BufferedImage s) { this.backgroundSnapshot = s; }
    public void loadAllThumbnails() { for(int i=0; i<4; i++) slotThumbnails[i] = controller.getSlotThumbnail(i+1); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        double s = (double)getHeight() / LOGIC_H;
        int cx = (int)((getWidth()/s)/2);

        if (backgroundSnapshot != null) g2.drawImage(backgroundSnapshot, 0, 0, getWidth(), getHeight(), null);
        g2.setColor(new Color(0, 0, 0, 180)); g2.fillRect(0, 0, getWidth(), getHeight());

        if (showSettings) {
            bgmSlider.setBounds((int)((cx + 10) * s), (int)(255 * s), (int)(150 * s), (int)(30 * s));
            sfxSlider.setBounds((int)((cx + 10) * s), (int)(315 * s), (int)(150 * s), (int)(30 * s));
            bgmSlider.setVisible(true); sfxSlider.setVisible(true);
        } else {
            bgmSlider.setVisible(false); sfxSlider.setVisible(false);
        }

        g2.scale(s, s);
        if (showSettings) drawSettingsUI(g2, cx); else drawPauseUI(g2, cx);
    }

    private void drawPauseUI(Graphics2D g2, int cx) {
        g2.setColor(Color.WHITE); g2.setFont(new Font("Arial", Font.BOLD, 40));
        g2.drawString("PAUSED", cx - g2.getFontMetrics().stringWidth("PAUSED")/2, 70);

        for (int i = 0; i < 4; i++) {
            int x = cx - 200;
            g2.setColor(new Color(50, 50, 50)); g2.fillRect(x, slotY[i], 400, 75);
            g2.setColor(Color.WHITE); g2.drawRect(x, slotY[i], 400, 75);
            if (slotThumbnails[i] != null) g2.drawImage(slotThumbnails[i], x + 5, slotY[i] + 5, 90, 65, null);

            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.setColor(hoveredSlot == i && isHoveringSave ? Color.YELLOW : Color.WHITE);
            g2.drawString("SAVE STATE", x + 110, slotY[i] + 25);
            g2.setColor(hoveredSlot == i && !isHoveringSave ? Color.YELLOW : Color.WHITE);
            g2.drawString("LOAD STATE", x + 110, slotY[i] + 55);
            g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawString(controller.getSlotMetadata(i+1), x + 210, slotY[i] + 42);
        }

        drawSmallBtn(g2, cx - 210, 460, "CONTINUE", Color.GREEN);
        drawSmallBtn(g2, cx + 10, 460, "SETTINGS", Color.BLUE);
        drawSmallBtn(g2, cx - 100, 515, "EXIT MENU", Color.RED);
    }

    private void drawSettingsUI(Graphics2D g2, int cx) {
        g2.setColor(Color.WHITE); g2.setFont(new Font("Arial", Font.BOLD, 40));
        g2.drawString("SETTINGS", cx - g2.getFontMetrics().stringWidth("SETTINGS")/2, 150);
        g2.setFont(new Font("Arial", Font.PLAIN, 20));
        g2.drawString("Music", cx - 140, 275);
        g2.drawString("Sound", cx - 140, 335);
        drawSmallBtn(g2, cx - 100, 460, "BACK", Color.GRAY);
    }

    private void drawSmallBtn(Graphics2D g2, int x, int y, String txt, Color c) {
        g2.setColor(c); g2.fillRect(x, y, 200, 45);
        g2.setColor(Color.WHITE); g2.drawRect(x, y, 200, 45);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.drawString(txt, x + (200 - g2.getFontMetrics().stringWidth(txt))/2, y + 28);
    }
}