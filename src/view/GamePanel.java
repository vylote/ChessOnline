package view;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.awt.LayoutManager;

import javax.swing.*;

import controller.GameController;
import controller.Mouse;
import model.Board;
import model.GameState;
import model.Piece;
import model.State; // Cần import model.State

public class GamePanel extends JPanel {

    // --- VIEW CONSTANTS ---
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 800;

    // --- MVC COMPONENTS ---
    private final GameController controller;
    private final Mouse mouse;
    private final MenuPanel menuPanel;      // NEW: Panel Menu
    private final PausePanel pausePanel;    // NEW: Panel Pause
    private final JButton pauseButton;      // NEW: Nút Pause
    private static final int PAUSE_BUTTON_SIZE = 40;

    // --- CONSTRUCTOR ---
    public GamePanel(GameController controller) {
        this.controller = controller;
        this.mouse = controller.mouse;

        // 1. Thiết lập Panel
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);

        // Sử dụng NULL LAYOUT để định vị nút Pause bằng tọa độ tuyệt đối
        setLayout(null);

        // 2. Khởi tạo các Panel View mới
        this.menuPanel = new MenuPanel(controller, this);
        this.pausePanel = new PausePanel(controller, this);

        // 3. Tạo và thêm Nút Pause
        pauseButton = new JButton("||"); // Sử dụng ký hiệu Pause
        pauseButton.setFont(new Font("Arial", Font.BOLD, 14)); // Giảm cỡ chữ
        pauseButton.setBackground(Color.WHITE); // Màu nền trắng

        int pauseX = WIDTH - PAUSE_BUTTON_SIZE;
        int pauseY = 0;
        // Đặt tọa độ tuyệt đối (Góc trên bên phải, ngoài bàn cờ)
        pauseButton.setBounds(pauseX, pauseY, PAUSE_BUTTON_SIZE, PAUSE_BUTTON_SIZE);

        pauseButton.addActionListener(e -> {
            if (GameState.currentState == State.PLAYING) {
                controller.pauseGame(); // Gọi hàm Controller khi ấn Pause
            }
        });
        this.add(pauseButton);

        // 4. Thêm listener (giữ nguyên logic input)
        addMouseMotionListener(mouse);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mouse.mousePressed(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                // Chỉ xử lý click-to-move khi game đang ở trạng thái PLAYING
                if (GameState.currentState == State.PLAYING) {
                    controller.handleMouseRelease(e.getX(), e.getY());
                }
                // MouseReleased sẽ được xử lý trong MenuPanel/PausePanel nếu trạng thái tương ứng
            }
        });
    }

    // --- VẼ GIAO DIỆN (VIEW) ---
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        // 1. Vẽ Board luôn
        controller.getBoard().draw(g2);

        // Ẩn/Hiện nút Pause dựa trên trạng thái
        pauseButton.setVisible(GameState.currentState == State.PLAYING);


        // 2. Điều hướng việc vẽ dựa trên trạng thái Game
        switch (GameState.currentState) {
            case MENU:
                menuPanel.draw(g2); // Vẽ Menu chính
                break;

            case PLAYING:
            case PAUSED:
                // Vẽ các thành phần game (quân cờ, thời gian, check)
                drawGame(g2);

                if (GameState.currentState == State.PAUSED) {
                    pausePanel.draw(g2); // Vẽ cửa sổ Tạm dừng lên trên
                }
                break;

            case GAME_OVER:
                // Vẽ game (để thấy thế cờ kết thúc)
                drawGame(g2);
                drawGameOver(g2); // Tách riêng logic vẽ Game Over
                break;
        }
    }

    /**
     * Phương thức mới: Chứa toàn bộ logic vẽ quân cờ, nước đi, thời gian, và check.
     */
    private void drawGame(Graphics2D g2) {

        Piece activeP = controller.getActiveP();
        ArrayList<Piece> simPieces = controller.getSimPieces();

        // --- Vẽ Ô được chọn và Nước đi hợp lệ ---
        if (activeP != null) {
            // 1. Vẽ ô vuông cho quân đang được chọn (activeP)
            g2.setColor(new Color(255, 165, 0));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
            g2.fillRect(activeP.preCol * Board.SQUARE_SIZE, activeP.preRow * Board.SQUARE_SIZE, Board.SQUARE_SIZE,
                    Board.SQUARE_SIZE);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

            // 2. Vẽ các nước đi hợp lệ
            if (controller.isClickedToMove()) {
                g2.setColor(new Color(0, 255, 0));
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));

                ArrayList<int[]> validMoves = controller.getValidMoves();
                for (int[] move : validMoves) {
                    g2.fillRect(move[0] * Board.SQUARE_SIZE, move[1] * Board.SQUARE_SIZE, Board.SQUARE_SIZE, Board.SQUARE_SIZE);
                }

                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            }
        }

        // --- Vẽ tất cả các quân cờ ---
        for (Piece p : simPieces) {
            p.draw(g2);
        }
        g2.setFont(new Font("Arial", Font.PLAIN, 40));

        // --- Vẽ Thời gian ---
        if (controller.isTimeRunning() && !controller.isGameOver()) {
            g2.setColor(Color.blue);
            String time = "Time left: " + controller.getTimeLeft();
            g2.drawString(time, Board.BOARD_WIDTH + 10, 400); // Tùy chỉnh tọa độ X
        }

        // --- Vẽ Quân đang hoạt động (Luôn vẽ sau cùng) ---
        if (activeP != null) {
            activeP.draw(g2);
        }

        // --- Vẽ Trạng thái Game (Check, Turn) ---
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(new Font("Arial", Font.PLAIN, 40));
        g2.setColor(Color.white);

        if (controller.isPromotion()) {
            g2.drawString("Promote to: ", Board.BOARD_WIDTH + 10, 150);
            for (Piece piece : controller.getPromoPieces()) {
                g2.drawImage(piece.image, piece.getX(piece.col), piece.getY(piece.row), Board.SQUARE_SIZE,
                        Board.SQUARE_SIZE, null);
            }
        } else {
            Piece king = controller.getKing(false);
            int currentColor = controller.getCurrentColor();
            Piece checkingP = controller.getCheckingP();

            // Vị trí cố định cho thông báo lượt đi/check (Cập nhật tọa độ X)
            int textX = Board.BOARD_WIDTH + 10;

            if (currentColor == GameController.WHITE) {
                if (checkingP != null) {
                    g2.setColor(Color.red);
                    g2.drawString("The white King", textX, 100);
                    g2.drawString("is in check", textX, 200);

                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                    g2.fillRect(king.col * Board.SQUARE_SIZE, king.row * Board.SQUARE_SIZE, Board.SQUARE_SIZE,
                            Board.SQUARE_SIZE);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                }

                g2.setColor(Color.white);
                g2.drawString("White turn", textX, 700);
            } else { // BLACK turn
                if (checkingP != null) {
                    g2.setColor(Color.red);
                    g2.drawString("The black King", textX, 600);
                    g2.drawString("is in check", textX, 700);

                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                    g2.fillRect(king.col * Board.SQUARE_SIZE, king.row * Board.SQUARE_SIZE, Board.SQUARE_SIZE,
                            Board.SQUARE_SIZE);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                }

                g2.setColor(Color.white);
                g2.drawString("Black turn", textX, 100);
            }
        }
    }

    /**
     * Phương thức mới: Chứa logic vẽ màn hình kết thúc game.
     */
    private void drawGameOver(Graphics2D g2) {
        String s;
        g2.setFont(new Font("Arial", Font.PLAIN, 100));

        // Vẽ màn hình phủ mờ
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, Board.BOARD_WIDTH, Board.BOARD_HEIGHT);

        if (controller.isDraw()) {
            g2.setColor(Color.MAGENTA);
            s = "Draw";
            g2.drawString(s, 280, 430);
        } else {
            g2.setColor(Color.green);
            // Người thắng là người KHÔNG có lượt đi hiện tại khi game kết thúc
            if (controller.getCurrentColor() == GameController.BLACK) {
                s = "white is the winner!";
            } else {
                s = "black is the winner!";
            }
            g2.drawString(s, 150, 420);
        }
    }
}