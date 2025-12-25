package view;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
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
        setBackground(Color.BLACK);
        setFocusable(true);

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
                updateCursor();
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    // --- PHƯƠNG THỨC CHỤP ẢNH MÀN HÌNH (Sửa lỗi compilation) ---
    public BufferedImage getGameSnapshot() {
        BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        this.printAll(g2d);
        g2d.dispose();
        return image;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double scale = (double) getHeight() / LOGIC_H;
        int boardPhysicalWidth = (int)(BOARD_W * scale);
        g2.setColor(new Color(25, 25, 25));
        g2.fillRect(boardPhysicalWidth, 0, getWidth() - boardPhysicalWidth, getHeight());

        drawPhysicalPauseButton(g2);

        g2.scale(scale, scale);
        controller.getBoard().draw(g2);
        drawGame(g2);
        drawSidebarText(g2);

        if (controller.isGameOver()) drawGameOver(g2);
    }

    private void drawGame(Graphics2D g2) {
        Piece activeP = controller.getActiveP();

        // 1. TÔ ĐỎ KING NGAY LẬP TỨC KHI BỊ CHIẾU
        if (controller.getCheckingP() != null) {
            Piece king = controller.getKing(false);
            if (king != null) {
                g2.setColor(new Color(255, 0, 0, 150));
                g2.fillRect(king.col * Board.SQUARE_SIZE, king.row * Board.SQUARE_SIZE, Board.SQUARE_SIZE, Board.SQUARE_SIZE);
            }
        }

        // 2. TÔ MÀU CÁC Ô DI CHUYỂN HỢP LỆ (HIGHLIGHT)
        if (activeP != null && controller.isClickedToMove()) {
            g2.setColor(new Color(255, 165, 0, 150)); // Ô quân đang chọn
            g2.fillRect(activeP.preCol * Board.SQUARE_SIZE, activeP.preRow * Board.SQUARE_SIZE, Board.SQUARE_SIZE, Board.SQUARE_SIZE);

            for (int[] move : controller.getValidMoves()) {
                int type = move[2]; // 0: Trống, 1: Ăn quân
                g2.setColor(type == 0 ? new Color(0, 255, 0, 100) : new Color(255, 0, 0, 100));
                g2.fillRect(move[0] * Board.SQUARE_SIZE, move[1] * Board.SQUARE_SIZE, Board.SQUARE_SIZE, Board.SQUARE_SIZE);
            }
        }

        for (Piece p : controller.getSimPieces()) p.draw(g2);
        if (activeP != null) activeP.draw(g2);
    }

    private void drawPhysicalPauseButton(Graphics2D g2) {
        if (GameState.currentState != State.PLAYING || controller.isGameOver()) return;
        int x = getWidth() - 60, y = 15, size = 45;
        g2.setColor(isHoveringPause ? new Color(90, 90, 90) : new Color(50, 50, 50));
        g2.fillRoundRect(x, y, size, size, 10, 10);
        g2.setColor(Color.WHITE); g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(x, y, size, size, 10, 10);
        g2.setFont(new Font("Arial", Font.BOLD, 20)); g2.drawString("||", x + 16, y + 30);
    }

    private void drawSidebarText(Graphics2D g2) {
        int x = BOARD_W + 20; g2.setColor(Color.WHITE); g2.setFont(new Font("Arial", Font.BOLD, 22));
        g2.drawString((controller.getCurrentColor() == 0 ? "White turn" : "Black turn"), x, 100);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 18));
        g2.drawString("TIME: " + controller.getTimeLeft(), x, 250);
    }

    private void updateCursor() {
        boolean h = isHoveringPause;
        if (!h && mouse.x < BOARD_W) {
            Piece p = getPieceAt(mouse.x / Board.SQUARE_SIZE, mouse.y / Board.SQUARE_SIZE);
            if (p != null && p.color == controller.getCurrentColor()) h = true;
        }
        setCursor(new Cursor(h ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private Piece getPieceAt(int c, int r) {
        for (Piece p : controller.getSimPieces()) if (p.col == c && p.row == r) return p;
        return null;
    }

    private void drawGameOver(Graphics2D g2) {
        // Phủ lớp nền mờ tối
        g2.setColor(new Color(0, 0, 0, 200));
        double s = (double) getHeight() / LOGIC_H;
        int totalLW = (int) (getWidth() / s);
        g2.fillRect(0, 0, totalLW, LOGIC_H);

        g2.setFont(new Font("Arial", Font.BOLD, 60));

        if (controller.isDraw()) {
            g2.setColor(Color.YELLOW);
            g2.drawString("DRAW GAME", totalLW / 2 - 180, 300);
        } else {
            g2.setColor(Color.GREEN);
            // Xác định bên thắng: Nếu đang lượt White mà Game Over (do hết giờ/chiếu hết) -> Black thắng
            String winner = (controller.getCurrentColor() == 0) ? "BLACK WINS!" : "WHITE WINS!";
            int xOffset = (winner.contains("BLACK")) ? 220 : 220; // Căn chỉnh chữ ra giữa
            g2.drawString(winner, totalLW / 2 - xOffset, 300);
        }

        g2.setFont(new Font("Arial", Font.PLAIN, 20));
        g2.setColor(Color.WHITE);
        g2.drawString("Press ESC to return to Menu", totalLW / 2 - 130, 360);
    }
}