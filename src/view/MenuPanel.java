package view;

import controller.GameController;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class MenuPanel extends JPanel {
    private static final int LOGIC_H = 600; // Chiều cao logic cố định
    private static final int BTN_W = 300;   // Chiều rộng nút
    private static final int BTN_H = 50;    // Chiều cao nút

    private final GameController controller;
    private BufferedImage backgroundImage;
    private BufferedImage[] slotThumbnails = new BufferedImage[4];
    private JDialog waitingDialog;

    private boolean isSettingsMode = false;
    private boolean isLoadMode = false;
    private int hoveredIndex = -1;

    private JSlider bgmSlider, sfxSlider;

    // Tọa độ Y cho các nút (cách nhau 60px)
    private final int playY = 160, multiY = 220, loadY = 280, settY = 340, exitY = 400, backY = 500;
    private final int[] slotY = {160, 240, 320, 400};

    public MenuPanel(GameController gc, JFrame frame) {
        this.controller = gc;
        setLayout(null);

        // Nạp ảnh nền
        try {
            backgroundImage = ImageIO.read(getClass().getResourceAsStream("/bg/menu_bg.png"));
        } catch (Exception e) { System.err.println("Menu BG not found."); }

        initSliders();

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                double s = (double)getHeight() / LOGIC_H;
                int lx = (int)(e.getX() / s), ly = (int)(e.getY() / s);
                int btnX = getCenteredBtnX();

                if (isLoadMode) {
                    for (int i = 0; i < 4; i++) {
                        if (lx >= btnX && lx <= btnX + BTN_W && ly >= slotY[i] && ly <= slotY[i] + 70) {
                            controller.loadGame(i + 1); return;
                        }
                    }
                    if (isInside(lx, ly, btnX, backY)) isLoadMode = false;
                } else if (isSettingsMode) {
                    if (isInside(lx, ly, btnX, backY)) isSettingsMode = false;
                } else {
                    if (isInside(lx, ly, btnX, playY)) controller.startNewGame();
                    else if (isInside(lx, ly, btnX, multiY)) showMultiplayerDialog();
                    else if (isInside(lx, ly, btnX, loadY)) { loadThumbnails(); isLoadMode = true; }
                    else if (isInside(lx, ly, btnX, settY)) isSettingsMode = true;
                    else if (isInside(lx, ly, btnX, exitY)) System.exit(0);
                }
                repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                double s = (double)getHeight() / LOGIC_H;
                int lx = (int)(e.getX() / s), ly = (int)(e.getY() / s);
                int btnX = getCenteredBtnX();
                hoveredIndex = -1;

                if (isLoadMode) {
                    for (int i = 0; i < 4; i++) if (lx >= btnX && lx <= btnX + BTN_W && ly >= slotY[i] && ly <= slotY[i] + 70) hoveredIndex = i;
                    if (isInside(lx, ly, btnX, backY)) hoveredIndex = 5;
                } else if (isSettingsMode) {
                    if (isInside(lx, ly, btnX, backY)) hoveredIndex = 5;
                } else {
                    if (isInside(lx, ly, btnX, playY)) hoveredIndex = 0;
                    else if (isInside(lx, ly, btnX, multiY)) hoveredIndex = 1;
                    else if (isInside(lx, ly, btnX, loadY)) hoveredIndex = 2;
                    else if (isInside(lx, ly, btnX, settY)) hoveredIndex = 3;
                    else if (isInside(lx, ly, btnX, exitY)) hoveredIndex = 4;
                }
                setCursor(new Cursor(hoveredIndex != -1 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                repaint();
            }
        };
        addMouseListener(ma); addMouseMotionListener(ma);
    }

    private void showMultiplayerDialog() {
        String[] options = {"Host Game", "Join Game", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this, "Chọn chế độ mạng", "Multiplayer", 0, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

        if (choice == 0) { // Host
            waitingDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Waiting...", true);
            JPanel p = new JPanel(new GridLayout(3, 1, 10, 10));
            p.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            p.add(new JLabel("IP: " + controller.getLocalIP()));
            p.add(new JLabel("Port: 5555"));
            JButton cb = new JButton("Cancel");
            cb.addActionListener(e -> { if(controller.netManager != null) controller.netManager.closeConnection(); waitingDialog.dispose(); });
            p.add(cb);
            waitingDialog.add(p); waitingDialog.pack(); waitingDialog.setLocationRelativeTo(this);

            controller.setupMultiplayer(true, 0, null);
            waitingDialog.setVisible(true);
        } else if (choice == 1) { // Join
            String ip = JOptionPane.showInputDialog(this, "IP Host:", "127.0.0.1");
            if (ip != null && !ip.isEmpty()) controller.setupMultiplayer(false, 1, ip);
        }
    }

    public void closeWaitingDialog() { if (waitingDialog != null) { waitingDialog.dispose(); waitingDialog = null; } }

    private void initSliders() {
        bgmSlider = new JSlider(0, 100);
        sfxSlider = new JSlider(0, 100);

        configureSlider(bgmSlider, controller.getAudioManager().getBGMVolumeAsInt());
        configureSlider(sfxSlider, controller.getAudioManager().getSFXVolumeAsInt());

        bgmSlider.addChangeListener(e -> controller.getAudioManager().setBGMVolumeFromSlider(bgmSlider.getValue()));
        sfxSlider.addChangeListener(e -> controller.getAudioManager().setSFXVolumeFromSlider(sfxSlider.getValue()));

        add(bgmSlider); add(sfxSlider);
    }

    private void configureSlider(JSlider slider, int value) {
        slider.setValue(value);
        slider.setOpaque(false);
        slider.setFocusable(false);
        // Tùy chỉnh màu sắc cơ bản cho Slider (tùy vào LookAndFeel)
        slider.setForeground(new Color(46, 204, 113));
    }

    private int getCenteredBtnX() { return (int)(((getWidth() / ((double)getHeight()/LOGIC_H)) / 2) - (BTN_W / 2)); }
    private boolean isInside(int lx, int ly, int bx, int by) { return lx >= bx && lx <= bx + BTN_W && ly >= by && ly <= by + BTN_H; }
    private void loadThumbnails() { for (int i = 0; i < 4; i++) slotThumbnails[i] = controller.getSlotThumbnail(i + 1); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        double s = (double)getHeight() / LOGIC_H;
        int cx = (int)((getWidth() / s) / 2), bx = cx - (BTN_W / 2);

        if (backgroundImage != null) g2.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null);
        g2.setColor(new Color(0, 0, 0, 160)); g2.fillRect(0, 0, getWidth(), getHeight());

        if (isSettingsMode) {
            // Căn chỉnh slider khớp với các hàng đã vẽ ở trên
            int sliderW = 360;
            bgmSlider.setBounds((int)((cx - sliderW/2) * s), (int)(315 * s), (int)(sliderW * s), (int)(20 * s));
            sfxSlider.setBounds((int)((cx - sliderW/2) * s), (int)(395 * s), (int)(sliderW * s), (int)(20 * s));
            bgmSlider.setVisible(true); sfxSlider.setVisible(true);
        } else { bgmSlider.setVisible(false); sfxSlider.setVisible(false); }

        g2.scale(s, s);
        if (isLoadMode) drawLoadUI(g2, cx, bx);
        else if (isSettingsMode) drawSettingsUI(g2, cx, bx);
        else drawMainUI(g2, cx, bx);
    }

    private void drawMainUI(Graphics2D g2, int cx, int bx) {
        drawTitle(g2, "CHESS GEMINI", cx);
        drawBtn(g2, bx, playY, "NEW GAME", new Color(46, 204, 113), 0);
        drawBtn(g2, bx, multiY, "MULTIPLAYER", new Color(155, 89, 182), 1);
        drawBtn(g2, bx, loadY, "LOAD GAME", new Color(52, 152, 219), 2);
        drawBtn(g2, bx, settY, "SETTINGS", new Color(149, 165, 166), 3);
        drawBtn(g2, bx, exitY, "EXIT", new Color(231, 76, 60), 4);
    }

    private void drawLoadUI(Graphics2D g2, int cx, int bx) {
        drawTitle(g2, "SELECT SLOT", cx);
        for (int i = 0; i < 4; i++) {
            g2.setColor(hoveredIndex == i ? new Color(80, 80, 80) : new Color(45, 45, 45));
            g2.fillRect(bx, slotY[i], BTN_W, 70);
            g2.setColor(Color.WHITE); g2.drawRect(bx, slotY[i], BTN_W, 70);
            if (slotThumbnails[i] != null) g2.drawImage(slotThumbnails[i], bx + 5, slotY[i] + 5, 90, 60, null);
            g2.drawString("SLOT " + (i+1), bx + 105, slotY[i] + 25);
            g2.setFont(new Font("Monospaced", 0, 11)); g2.drawString(controller.getSlotMetadata(i+1), bx + 105, slotY[i] + 55);
        }
        drawBtn(g2, bx, backY, "BACK", Color.GRAY, 5);
    }

    private void drawSettingsUI(Graphics2D g2, int cx, int bx) {
        drawTitle(g2, "SETTINGS", cx);

        int panelW = 400;
        int panelX = cx - panelW / 2;

        // Vẽ khung bao quanh từng mục setting
        drawSettingRow(g2, panelX, 280, panelW, "MUSIC", bgmSlider.getValue() + "%");
        drawSettingRow(g2, panelX, 360, panelW, "SOUND FX", sfxSlider.getValue() + "%");

        drawBtn(g2, bx, backY, "BACK", Color.GRAY, 5);
    }

    private void drawSettingRow(Graphics2D g2, int x, int y, int w, String label, String val) {
        // Vẽ nền mờ cho mỗi hàng
        g2.setColor(new Color(255, 255, 255, 20));
        g2.fillRoundRect(x, y, w, 60, 10, 10);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g2.drawString(label, x + 20, y + 25);

        g2.setFont(new Font("Monospaced", Font.BOLD, 14));
        g2.setColor(new Color(46, 204, 113));
        g2.drawString(val, x + w - 50, y + 25);
    }

    private void drawTitle(Graphics2D g2, String t, int cx) {
        g2.setColor(Color.WHITE); g2.setFont(new Font("Arial", 1, 50));
        g2.drawString(t, cx - g2.getFontMetrics().stringWidth(t)/2, 100);
    }

    private void drawBtn(Graphics2D g2, int x, int y, String t, Color c, int idx) {
        g2.setColor(hoveredIndex == idx ? c.brighter() : c); g2.fillRect(x, y, BTN_W, BTN_H);
        g2.setColor(Color.WHITE); g2.setFont(new Font("Arial", 1, 18));
        g2.drawString(t, x + (BTN_W - g2.getFontMetrics().stringWidth(t))/2, y + 32);
    }

    public void resetMenu() { isLoadMode = false; isSettingsMode = false; repaint(); }
}