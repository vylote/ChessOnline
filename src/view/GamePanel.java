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

    // Các biến phụ trợ để tính toán vùng nút Pause
    private int dynamicPauseX = 0;
    private final int pauseSize = 45;
    private boolean isHoveringPause = false;

    public GamePanel(GameController controller) {
        this.controller = controller;
        this.mouse = controller.mouse;
        setLayout(null);
        setBackground(Color.BLACK);
        setFocusable(true);

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateMouseAndHover(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                updateMouseAndHover(e);
                // Xử lý Click nút Pause bằng tọa độ thực để chính xác tuyệt đối
                if (GameState.currentState == State.PLAYING && isHoveringPause) {
                    controller.pauseGame();
                } else {
                    mouse.pressed = true;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                updateMouseAndHover(e);
                if (!isHoveringPause) {
                    controller.handleMouseRelease(mouse.x, mouse.y);
                }
                mouse.pressed = false;
            }

            // Hàm cập nhật tọa độ chuột và kiểm tra hover nút Pause
            private void updateMouseAndHover(MouseEvent e) {
                double scale = (double) getHeight() / LOGIC_H;
                // Ánh xạ chuột về hệ logic 600
                mouse.updateLocation((int)(e.getX() / scale), (int)(e.getY() / scale));

                // Kiểm tra Hover dựa trên tọa độ vật lý (để nút dính lề phải chuẩn nhất)
                int px = e.getX();
                int py = e.getY();
                boolean prevHover = isHoveringPause;

                // Nút Pause nằm tại: [getWidth() - 60] tới [getWidth() - 15]
                isHoveringPause = (px >= getWidth() - 60 && px <= getWidth() - 15 && py >= 15 && py <= 60);

                if (prevHover != isHoveringPause) repaint();
                updateCursor();
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double scale = (double) getHeight() / LOGIC_H;

        // 1. Vẽ nền Sidebar phủ kín 100% bên phải bàn cờ
        int boardPhysicalWidth = (int)(BOARD_W * scale);
        g2.setColor(new Color(25, 25, 25));
        g2.fillRect(boardPhysicalWidth, 0, getWidth() - boardPhysicalWidth, getHeight());

        // 2. Vẽ nút Pause tại tọa độ VẬT LÝ (TRƯỚC KHI SCALE) để nó luôn ở góc phải màn hình
        drawPhysicalPauseButton(g2);

        // 3. Bắt đầu SCALE để vẽ bàn cờ và nội dung game
        g2.scale(scale, scale);

        controller.getBoard().draw(g2);
        drawGame(g2);
        drawSidebarText(g2);

        if (controller.isGameOver()) drawGameOver(g2);
    }

    private void drawPhysicalPauseButton(Graphics2D g2) {
        if (GameState.currentState != State.PLAYING || controller.isGameOver()) return;

        // Tính toán vị trí góc trên cùng bên phải cửa sổ
        int x = getWidth() - 60;
        int y = 15;
        int size = 45;

        // Nền nút
        g2.setColor(isHoveringPause ? new Color(90, 90, 90) : new Color(50, 50, 50));
        g2.fillRoundRect(x, y, size, size, 10, 10);

        // Viền nút
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(x, y, size, size, 10, 10);

        // Biểu tượng ||
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        g2.drawString("||", x + 16, y + 30);
    }

    private void drawSidebarText(Graphics2D g2) {
        int x = BOARD_W + 20;
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 22));
        String turn = (controller.getCurrentColor() == 0) ? "WHITE TURN" : "BLACK TURN";
        g2.drawString(turn, x, 100);

        g2.setFont(new Font("Monospaced", Font.PLAIN, 18));
        g2.drawString("TIME: " + controller.getTimeLeft(), x, 250);
    }

    // --- Các hàm drawGame, updateCursor, getPieceAt, drawGameOver giữ nguyên ---
    private void drawGame(Graphics2D g2) {
        for (Piece p : controller.getSimPieces()) p.draw(g2);
        if (controller.getActiveP() != null) {
            if (controller.isClickedToMove()) {
                for (int[] move : controller.getValidMoves()) {
                    g2.setColor(move[2] == 0 ? new Color(0, 255, 0, 100) : new Color(255, 0, 0, 100));
                    g2.fillRect(move[0] * Board.SQUARE_SIZE, move[1] * Board.SQUARE_SIZE, Board.SQUARE_SIZE, Board.SQUARE_SIZE);
                }
            }
            controller.getActiveP().draw(g2);
        }
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
        g2.setColor(new Color(0, 0, 0, 180));
        double scale = (double) getHeight() / LOGIC_H;
        int totalLW = (int) (getWidth() / scale);
        g2.fillRect(0, 0, totalLW, LOGIC_H);
        g2.setColor(Color.YELLOW);
        g2.setFont(new Font("Arial", Font.BOLD, 50));
        g2.drawString("GAME OVER", totalLW / 2 - 150, 300);
    }

    public BufferedImage getGameSnapshot() {
        BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        this.paint(g2d);
        g2d.dispose();
        return image;
    }
}