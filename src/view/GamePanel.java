package view;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javax.swing.*;
import controller.GameController;
import controller.Mouse;
import model.*;

public class GamePanel extends JPanel {
    public static final int LOGIC_H = 600;
    public static final int BOARD_W = 600;

    private final GameController controller;
    private final Mouse mouse;
    private boolean isHoveringPause = false;

    public GamePanel(GameController controller) {
        this.controller = controller;
        this.mouse = controller.mouse;
        setLayout(null);
        setBackground(new Color(15, 15, 15));
        setFocusable(true);
        setupKeyBindings();

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) { updateMouseAndHover(e); }
            @Override
            public void mousePressed(MouseEvent e) {
                updateMouseAndHover(e);
                if (GameState.currentState == State.PLAYING && isHoveringPause) {
                    controller.pauseGame();
                } else { mouse.pressed = true; }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                updateMouseAndHover(e);
                if (!isHoveringPause) { controller.handleMouseRelease(mouse.x, mouse.y); }
                mouse.pressed = false;
            }
            private void updateMouseAndHover(MouseEvent e) {
                double scale = (double) getHeight() / LOGIC_H;
                mouse.updateLocation((int)(e.getX() / scale), (int)(e.getY() / scale));
                int px = e.getX(); int py = e.getY();
                isHoveringPause = (px >= getWidth() - 60 && px <= getWidth() - 15 && py >= 15 && py <= 60);
                repaint();
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    private void setupKeyBindings() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escAction");
        am.put("escAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (controller.isGameOver()) controller.exitToMenu();
                else controller.pauseGame();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double scale = (double) getHeight() / LOGIC_H;

        // Vẽ Sidebar Background
        int boardPhysicalWidth = (int)(BOARD_W * scale);
        g2.setColor(new Color(30, 30, 30));
        g2.fillRect(boardPhysicalWidth, 0, getWidth() - boardPhysicalWidth, getHeight());

        drawPhysicalPauseButton(g2);

        // Chuyển sang hệ tọa độ Logic
        g2.scale(scale, scale);

        // --- CẬP NHẬT: VẼ BÀN CỜ ĐẢO NGƯỢC ---
        // Bàn cờ lật khi là Multiplayer và người chơi là quân Đen
        boolean isFlipped = controller.isMultiplayer && controller.playerColor == GameController.BLACK;
        controller.getBoard().draw(g2, isFlipped);

        drawPiecesAndEffects(g2);
        drawPlayerInfo(g2);

        if (controller.isGameOver()) drawGameOver(g2);
    }

    private void drawPiecesAndEffects(Graphics2D g2) {
        int size = Board.SQUARE_SIZE;

        // Highlight King khi bị chiếu
        if (controller.getCheckingP() != null) {
            Piece king = controller.getKing(false);
            if (king != null) {
                g2.setColor(new Color(255, 0, 0, 150));
                g2.fillRect(controller.getDisplayCol(king.col) * size,
                        controller.getDisplayRow(king.row) * size, size, size);
            }
        }

        // Highlight nước đi hợp lệ
        Piece activeP = controller.getActiveP();
        if (activeP != null && controller.isClickedToMove() && !controller.isPromotion()) {
            g2.setColor(new Color(255, 165, 0, 120));
            g2.fillRect(controller.getDisplayCol(activeP.col) * size,
                    controller.getDisplayRow(activeP.row) * size, size, size);

            for (int[] move : controller.getValidMoves()) {
                g2.setColor(move[2] == 0 ? new Color(0, 255, 0, 100) : new Color(255, 0, 0, 100));
                g2.fillRect(controller.getDisplayCol(move[0]) * size,
                        controller.getDisplayRow(move[1]) * size, size, size);
            }
        }

        // Vẽ các quân cờ
        for (Piece p : controller.getSimPieces()) {
            g2.drawImage(p.image, controller.getDisplayCol(p.col) * size,
                    controller.getDisplayRow(p.row) * size, size, size, null);
        }

        if (controller.isPromotion()) drawPromotionUI(g2);
    }

    private void drawPlayerInfo(Graphics2D g2) {
        int boardRight = BOARD_W + 20;
        g2.setFont(new Font("Segoe UI", Font.BOLD, 18));

        // Đối thủ (Phía trên)
        g2.setColor(new Color(45, 45, 45));
        g2.fillRoundRect(boardRight, 20, 160, 80, 15, 15);
        g2.setColor(Color.WHITE);
        g2.drawString("Opponent", boardRight + 50, 45);
        g2.setColor(controller.playerColor == GameController.WHITE ? Color.BLACK : Color.WHITE);
        g2.fillOval(boardRight + 15, 30, 25, 25);

        // Bạn (Phía dưới)
        g2.setColor(new Color(60, 60, 60));
        g2.fillRoundRect(boardRight, 500, 160, 80, 15, 15);
        g2.setColor(new Color(0, 255, 100));
        g2.drawString("You (" + (controller.isServer ? "Host" : "Join") + ")", boardRight + 45, 525);
        g2.setColor(controller.playerColor == GameController.WHITE ? Color.WHITE : Color.BLACK);
        g2.fillOval(boardRight + 15, 510, 25, 25);

        // Turn & Time
        g2.setColor(Color.ORANGE);
        g2.drawString((controller.getCurrentColor() == 0 ? "WHITE" : "BLACK") + "'S TURN", boardRight, 250);
        g2.setColor(Color.WHITE);
        g2.drawString("TIME: " + controller.getTimeLeft() + "s", boardRight, 280);

        if (controller.getCurrentColor() == controller.playerColor) {
            g2.setColor(Color.GREEN);
            g2.setStroke(new BasicStroke(3));
            g2.drawRoundRect(boardRight, 500, 160, 80, 15, 15);
        }
    }

    private void drawPromotionUI(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, BOARD_W, LOGIC_H);
        ArrayList<Piece> promoPieces = controller.getPromoPieces();
        int size = Board.SQUARE_SIZE;
        for (Piece p : promoPieces) {
            int x = controller.getDisplayCol(p.col) * size;
            int y = controller.getDisplayRow(p.row) * size;
            g2.setColor(new Color(139, 139, 139));
            g2.fillRect(x, y, size, size);
            g2.drawImage(p.image, x, y, size, size, null);
        }
    }

    private void drawPhysicalPauseButton(Graphics2D g2) { if (GameState.currentState != State.PLAYING || controller.isGameOver()) return; int x = getWidth() - 55, y = 10, size = 45; g2.setColor(isHoveringPause ? new Color(90, 90, 90) : new Color(50, 50, 50)); g2.fillRoundRect(x, y, size, size, 10, 10); g2.setColor(Color.WHITE); g2.setStroke(new BasicStroke(2)); g2.drawRoundRect(x, y, size, size, 10, 10); g2.setFont(new Font("Arial", Font.BOLD, 20)); g2.drawString("||", x + 16, y + 30); }
    private void drawGameOver(Graphics2D g2) { g2.setColor(new Color(0, 0, 0, 200)); double s = (double) getHeight() / LOGIC_H; int totalLW = (int) (getWidth() / s); g2.fillRect(0, 0, totalLW, LOGIC_H); g2.setFont(new Font("Arial", Font.BOLD, 60)); if (controller.isDraw()) { g2.setColor(Color.YELLOW); String msg = "DRAW GAME"; g2.drawString(msg, totalLW / 2 - g2.getFontMetrics().stringWidth(msg)/2, 300); } else { g2.setColor(Color.GREEN); String winner = (controller.getCurrentColor() == 0) ? "BLACK WINS!" : "WHITE WINS!"; g2.drawString(winner, totalLW / 2 - g2.getFontMetrics().stringWidth(winner)/2, 300); } g2.setFont(new Font("Arial", Font.PLAIN, 20)); g2.setColor(Color.WHITE); String subMsg = "Press ESC to return to Menu"; g2.drawString(subMsg, totalLW / 2 - g2.getFontMetrics().stringWidth(subMsg)/2, 360); }
    public BufferedImage getGameSnapshot() { BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB); Graphics2D g2d = image.createGraphics(); this.printAll(g2d); g2d.dispose(); return image; }
}