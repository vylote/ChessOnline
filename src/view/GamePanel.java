package view;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javax.swing.*;
import controller.core.GameController;
import controller.Mouse;
import model.*;

public class GamePanel extends JPanel {
    // --- CẤU HÌNH HỆ TỌA ĐỘ LOGIC ---
    public static final int LOGIC_H = 600;
    public static final int BOARD_W = 600;

    private final GameController controller;
    private final Mouse mouse;
    private boolean isHoveringPause = false;

    // --- CÁC THÀNH PHẦN UI ---
    private JButton btnRematch;
    private JButton btnExit;

    public GamePanel(GameController controller) {
        this.controller = controller;
        this.mouse = controller.mouse;
        setLayout(null); // Sử dụng null để tự do đặt vị trí nút bấm
        setBackground(new Color(15, 15, 15));
        setFocusable(true);

        initGameOverButtons();
        setupKeyBindings();

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) { updateMouseAndHover(e); }
            @Override
            public void mousePressed(MouseEvent e) {
                updateMouseAndHover(e);
                // Nếu game đã kết thúc, không cho phép click chuột xuống bàn cờ
                if (controller.isGameOver()) return;

                if (GameState.currentState == State.PLAYING && isHoveringPause) {
                    controller.pauseGame();
                } else { mouse.pressed = true; }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                updateMouseAndHover(e);
                if (controller.isGameOver()) return;

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

    /** Khởi tạo các nút bấm xuất hiện khi ván đấu kết thúc */
    private void initGameOverButtons() {
        btnRematch = new JButton("PLAY AGAIN");
        btnExit = new JButton("EXIT TO MENU");

        styleButton(btnRematch, new Color(46, 204, 113)); // Xanh lá
        styleButton(btnExit, new Color(231, 76, 60));    // Đỏ

        btnRematch.addActionListener(e -> {
            hideGameOverButtons();
            controller.requestRematch();
        });

        btnExit.addActionListener(e -> {
            hideGameOverButtons();
            controller.exitToMenu();
        });

        add(btnRematch);
        add(btnExit);
        hideGameOverButtons();
    }

    private void styleButton(JButton btn, Color bg) {
        btn.setFocusPainted(false);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btn.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void hideGameOverButtons() {
        btnRematch.setVisible(false);
        btnExit.setVisible(false);
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

        // 1. Vẽ Sidebar Background (Vùng thông tin bên phải)
        int boardPhysicalWidth = (int)(BOARD_W * scale);
        g2.setColor(new Color(30, 30, 30));
        g2.fillRect(boardPhysicalWidth, 0, getWidth() - boardPhysicalWidth, getHeight());

        drawPhysicalPauseButton(g2);

        // 2. Chuyển sang hệ tọa độ Logic để vẽ bàn cờ và quân cờ
        g2.scale(scale, scale);

        boolean isFlipped = controller.isMultiplayer && controller.playerColor == GameController.BLACK;
        controller.getBoard().draw(g2, isFlipped);

        drawPiecesAndEffects(g2);
        drawPlayerInfo(g2);

        if (controller.getToastAlpha() > 0) drawToast(g2);

        // 3. Vẽ lớp phủ Game Over (Nếu kết thúc)
        if (controller.isGameOver()) {
            drawGameOver(g2);
        } else {
            hideGameOverButtons();
        }
    }

    private void drawGameOver(Graphics2D g2) {
        // Vẽ lớp phủ tối mờ
        g2.setColor(new Color(0, 0, 0, 200));
        double s = (double) getHeight() / LOGIC_H;
        int totalLW = (int) (getWidth() / s);
        g2.fillRect(0, 0, totalLW, LOGIC_H);

        // Hiển thị kết quả thắng/thua
        g2.setFont(new Font("Arial", Font.BOLD, 60));
        String msg;
        if (controller.isDraw()) {
            g2.setColor(Color.YELLOW);
            msg = "DRAW GAME";
        } else {
            g2.setColor(Color.WHITE);
            // Ở đây currentColor là người vừa kết thúc lượt -> đối thủ thắng
            msg = (controller.getCurrentColor() == GameController.WHITE) ? "BLACK WINS!" : "WHITE WINS!";
        }
        g2.drawString(msg, totalLW / 2 - g2.getFontMetrics().stringWidth(msg)/2, 250);

        // Đặt vị trí các nút bấm (Phải tính theo hệ tọa độ Pixel thực tế)
        int centerX = getWidth() / 2;
        int buttonY = (int) (320 * s);

        btnRematch.setBounds(centerX - 160, buttonY, 150, 50);
        btnExit.setBounds(centerX + 10, buttonY, 150, 50);

        btnRematch.setVisible(true);
        btnExit.setVisible(true);
    }

    private void drawToast(Graphics2D g2) {
        String msg = controller.toastMsg; // Lấy tin nhắn trực tiếp từ Controller
        if (msg.isEmpty()) return;

        g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
        FontMetrics fm = g2.getFontMetrics();
        int x = (BOARD_W - fm.stringWidth(msg)) / 2;
        int y = 300;

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, controller.getToastAlpha()));
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRoundRect(x - 20, y - 35, fm.stringWidth(msg) + 40, 50, 15, 15);
        g2.setColor(Color.WHITE);
        g2.drawString(msg, x, y);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    private void drawPiecesAndEffects(Graphics2D g2) {
        int size = Board.SQUARE_SIZE;

        if (controller.getCheckingP() != null) {
            // Phe bị chiếu là đối thủ của quân đang gây chiếu (checkingP)
            int colorInCheck = (controller.getCheckingP().color == GameController.WHITE) ? GameController.BLACK : GameController.WHITE;
            Piece king = controller.getKingByColor(colorInCheck);

            if (king != null) {
                g2.setColor(new Color(255, 0, 0, 180)); // Màu đỏ đậm rực rỡ
                g2.fillRect(controller.getDisplayCol(king.col) * Board.SQUARE_SIZE,
                        controller.getDisplayRow(king.row) * Board.SQUARE_SIZE,
                        Board.SQUARE_SIZE, Board.SQUARE_SIZE);
            }
        }

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

        for (Piece p : controller.getSimPieces()) {
            g2.drawImage(p.image, controller.getDisplayCol(p.col) * size,
                    controller.getDisplayRow(p.row) * size, size, size, null);
        }

        if (controller.isPromotion()) drawPromotionUI(g2);
    }

    private void drawPlayerInfo(Graphics2D g2) {
        int boardRight = BOARD_W + 20;
        int curColor = controller.getCurrentColor();
        int myColor = controller.playerColor;

        g2.setFont(new Font("Segoe UI", Font.BOLD, 18));

        // --- KHỐI ĐỐI THỦ (Phía trên) ---
        g2.setColor(new Color(45, 45, 45));
        g2.fillRoundRect(boardRight, 20, 160, 80, 15, 15);

        // Nếu là lượt đối thủ -> Vẽ viền trắng nhẹ hoặc cam báo hiệu
        if (curColor != myColor) {
            g2.setColor(new Color(255, 165, 0)); // Màu cam nhấn mạnh lượt đối thủ
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(boardRight, 20, 160, 80, 15, 15);
        }

        g2.setColor(Color.WHITE);
        g2.drawString("Opponent", boardRight + 50, 45);
        g2.setColor(myColor == GameController.WHITE ? Color.BLACK : Color.WHITE);
        g2.fillOval(boardRight + 15, 30, 25, 25);

        // --- KHỐI CỦA BẠN (Phía dưới) ---
        g2.setColor(new Color(60, 60, 60));
        g2.fillRoundRect(boardRight, 500, 160, 80, 15, 15);

        // CHỖ THAY ĐỔI: Chỉ vẽ viền xanh khi là lượt của bạn
        if (curColor == myColor) {
            g2.setColor(new Color(46, 204, 113)); // Xanh lá chuyên nghiệp
            g2.setStroke(new BasicStroke(3));
            g2.drawRoundRect(boardRight, 500, 160, 80, 15, 15);
        }

        g2.setColor(new Color(0, 255, 100));
        String role = controller.isMultiplayer ? (controller.isServer ? "Host" : "Join") : "Local";
        g2.drawString("You (" + role + ")", boardRight + 45, 525);
        g2.setColor(myColor == GameController.WHITE ? Color.WHITE : Color.BLACK);
        g2.fillOval(boardRight + 15, 510, 25, 25);

        // --- THÔNG TIN TRẠNG THÁI ---
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        g2.setColor(Color.GRAY);
        g2.drawString("STATUS", boardRight, 230);

        g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
        g2.setColor(curColor == 0 ? Color.WHITE : new Color(180, 180, 180));
        String turnText = (curColor == myColor) ? "YOUR TURN" : "WAITING...";
        g2.drawString(turnText, boardRight, 255);

        // Đồng hồ (Time Left)
        int time = controller.getTimeLeft();
        g2.setFont(new Font("Monospaced", Font.BOLD, 30));
        g2.setColor(time <= 5 ? Color.RED : Color.WHITE);
        g2.drawString(String.format("%2d", time), boardRight, 300);
    }

    // Tối ưu drawPromotionUI để tính đúng tọa độ click cho quân Đen
    private void drawPromotionUI(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, BOARD_W, LOGIC_H);

        ArrayList<Piece> promoPieces = controller.getPromoPieces();
        int size = Board.SQUARE_SIZE;
        Piece activeP = controller.getActiveP();

        // Tọa độ vẽ hiển thị trên màn hình
        int displayCol = controller.getDisplayCol(activeP.col);
        int displayRow = controller.getDisplayRow(activeP.row);

        for (int i = 0; i < promoPieces.size(); i++) {
            int y = (activeP.color == GameController.WHITE) ? (displayRow + i) * size : (displayRow - i) * size;
            if (y < 0) y = 0; if (y > LOGIC_H - size) y = LOGIC_H - size;

            // Vẽ ô slot
            g2.setColor(new Color(240, 240, 240, 220));
            g2.fillRoundRect(displayCol * size + 5, y + 5, size - 10, size - 10, 10, 10);
            g2.drawImage(promoPieces.get(i).image, displayCol * size, y, size, size, null);
        }
    }

    private void drawPhysicalPauseButton(Graphics2D g2) {
        if (GameState.currentState != State.PLAYING || controller.isGameOver()) return;
        int x = getWidth() - 55, y = 10, size = 45;
        g2.setColor(isHoveringPause ? new Color(90, 90, 90) : new Color(50, 50, 50));
        g2.fillRoundRect(x, y, size, size, 10, 10);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(x, y, size, size, 10, 10);
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        g2.drawString("||", x + 16, y + 30);
    }

    public BufferedImage getGameSnapshot() {
        BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        this.printAll(g2d);
        g2d.dispose();
        return image;
    }
}