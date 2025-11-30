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

import javax.swing.JPanel;

import controller.GameController;
import controller.Mouse;
import model.Board;
import model.Piece;

public class GamePanel extends JPanel { // KHÔNG còn implements Runnable

    // --- VIEW CONSTANTS (Giữ lại kích thước hiển thị) ---
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 800;

    // --- CONTROLLER REFERENCE ---
    private final GameController controller;
    private final Mouse mouse; // Giữ lại Mouse để add listener

    // --- CONSTRUCTOR ---
    public GamePanel(GameController controller) {
        this.controller = controller;
        this.mouse = controller.mouse; // Lấy Mouse object từ Controller

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);

        // Thêm listener cho đối tượng Mouse của Controller
        addMouseMotionListener(mouse);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mouse.mousePressed(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                // Gửi sự kiện release và tọa độ đến Controller
                controller.handleMouseRelease(e.getX(), e.getY());
            }
            // mouseDragged không cần thiết trong chế độ click-to-click
        });
    }

    // --- VẼ GIAO DIỆN (VIEW) ---
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        Board board = controller.getBoard(); // Lấy Board từ Controller
        board.draw(g2);

        Piece activeP = controller.getActiveP();

        // --- Vẽ Ô được chọn và Nước đi hợp lệ ---
        if (activeP != null) {
            // 1. Vẽ ô vuông cho quân đang được chọn (activeP)
            g2.setColor(new Color(255, 165, 0)); // Orange for selected piece
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
            g2.fillRect(activeP.preCol * Board.SQUARE_SIZE, activeP.preRow * Board.SQUARE_SIZE, Board.SQUARE_SIZE,
                    Board.SQUARE_SIZE);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

            // 2. Vẽ các nước đi hợp lệ
            if (controller.isClickedToMove()) {
                g2.setColor(new Color(0, 255, 0)); // Green for valid moves
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));

                ArrayList<int[]> validMoves = controller.getValidMoves();
                for (int[] move : validMoves) {
                    g2.fillRect(move[0] * Board.SQUARE_SIZE, move[1] * Board.SQUARE_SIZE, Board.SQUARE_SIZE, Board.SQUARE_SIZE);
                }

                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            }
        }

        // --- Vẽ tất cả các quân cờ ---
        for (Piece p : controller.getSimPieces()) {
            p.draw(g2);
        }
        g2.setFont(new Font("Arial", Font.PLAIN, 40));

        // --- Vẽ Thời gian ---
        if (!controller.isGameOver()) {
            g2.setColor(Color.blue);
            String time = "Time left: " + controller.getTimeLeft();
            g2.drawString(time, 890, 400);
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
            g2.drawString("Promote to: ", 900, 150);
            for (Piece piece : controller.getPromoPieces()) {
                g2.drawImage(piece.image, piece.getX(piece.col), piece.getY(piece.row), Board.SQUARE_SIZE,
                        Board.SQUARE_SIZE, null);
            }
        } else {
            Piece king = controller.getKing(false); // Cần getter getKing(boolean) trong Controller
            int currentColor = controller.getCurrentColor();
            Piece checkingP = controller.getCheckingP();

            if (currentColor == GameController.WHITE) {
                if (checkingP != null && checkingP.color == GameController.BLACK) {
                    g2.setColor(Color.red);
                    g2.drawString("The white King", 860, 100);
                    g2.drawString("is in check", 900, 200);

                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                    g2.fillRect(king.col * Board.SQUARE_SIZE, king.row * Board.SQUARE_SIZE, Board.SQUARE_SIZE,
                            Board.SQUARE_SIZE);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                }

                g2.setColor(Color.white);
                g2.drawString("White turn", 900, 700);
            } else {
                if (checkingP != null && checkingP.color == GameController.WHITE) {
                    g2.setColor(Color.red);
                    g2.drawString("The black King", 860, 600);
                    g2.drawString("is in check", 900, 700);

                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                    g2.fillRect(king.col * Board.SQUARE_SIZE, king.row * Board.SQUARE_SIZE, Board.SQUARE_SIZE,
                            Board.SQUARE_SIZE);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                }

                g2.setColor(Color.white);
                g2.drawString("Black turn", 900, 100);
            }
        }

        // --- Vẽ Màn hình Kết thúc Game ---
        if (controller.isGameOver()) {
            String s;
            g2.setFont(new Font("Arial", Font.PLAIN, 100));
            if (controller.isDraw()) {
                g2.setColor(Color.MAGENTA);
                s = "Draw";
                g2.drawString(s, 280, 430);
            } else {
                g2.setColor(Color.green);
                if (controller.getCurrentColor() == GameController.BLACK) {
                    s = "white is the winner!";
                } else {
                    s = "black is the winner!";
                }
                g2.drawString(s, 150, 420);
            }
        }
    }
}