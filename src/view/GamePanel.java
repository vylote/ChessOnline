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

    // --- VIEW CONSTANTS ---
    public static final int WIDTH = 1200;
    public static final int HEIGHT = 800;

    // --- MVC COMPONENTS ---
    private final GameController controller;
    private final Mouse mouse;

    private final JButton pauseButton;
    private static final int PAUSE_BUTTON_SIZE = 40;

    // --- CONSTRUCTOR ---
    public GamePanel(GameController controller) {
        this.controller = controller;
        this.mouse = controller.mouse;

        setFocusable(true);
        requestFocusInWindow();

        // 1. Thiết lập Panel
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setLayout(null);

        // 2. Tạo và thêm Nút Pause
        pauseButton = new JButton("||");
        pauseButton.setFont(new Font("Arial", Font.BOLD, 14));
        pauseButton.setBackground(Color.BLACK);
        pauseButton.setFocusable(false); // Tránh cướp focus của KeyListener
        pauseButton.setBorder(BorderFactory.createLineBorder(Color.WHITE));
        pauseButton.setForeground(Color.WHITE);
        pauseButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        int pauseX = WIDTH - PAUSE_BUTTON_SIZE-1;
        int pauseY = 1;
        pauseButton.setBounds(pauseX, pauseY, PAUSE_BUTTON_SIZE, PAUSE_BUTTON_SIZE);

        pauseButton.addActionListener(e -> {
            if (GameState.currentState == State.PLAYING) {
                controller.pauseGame();
            }
        });
        this.add(pauseButton);

        // 3. Listeners
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    handleEscapeKey();
                }
            }
        });

        addMouseMotionListener(mouse);
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();

                // Chuyển đổi tọa độ pixel sang tọa độ bàn cờ
                int col = x / Board.SQUARE_SIZE;
                int row = y / Board.SQUARE_SIZE;

                boolean canInteract = false;

                // 1. Kiểm tra hover vào nút Pause
                if (pauseButton.getBounds().contains(x, y) && pauseButton.isVisible()) {
                    canInteract = true;
                }

                // 2. Kiểm tra hover vào quân cờ (phải đúng lượt đi)
                if (!canInteract && GameState.currentState == State.PLAYING) {
                    Piece p = getPieceAt(col, row);
                    if (p != null && p.color == controller.getCurrentColor()) {
                        canInteract = true;
                    }
                }

                // 3. Kiểm tra hover vào ô đang tô xanh (nước đi hợp lệ)
                if (!canInteract && GameState.currentState == State.PLAYING && controller.isClickedToMove()) {
                    ArrayList<int[]> validMoves = controller.getValidMoves();
                    for (int[] move : validMoves) {
                        if (move[0] == col && move[1] == row) {
                            canInteract = true;
                            break;
                        }
                    }
                }

                // --- THỰC THI ĐỔI CURSOR ---
                if (canInteract) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mouse.mousePressed(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (GameState.currentState == State.PLAYING) {
                    controller.handleMouseRelease(e.getX(), e.getY());
                }
            }
        });
    }

    private void handleEscapeKey() {
        if (GameState.currentState == State.PLAYING) {
            controller.pauseGame();
        } else if (GameState.currentState == State.PAUSED) {
            controller.resumeGame();
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Vẽ Board
        controller.getBoard().draw(g2);

        // Cập nhật trạng thái nút Pause
        pauseButton.setVisible(GameState.currentState == State.PLAYING && !controller.isGameOver());

        if (GameState.currentState == State.MENU) return;

        // Vẽ logic Game chính (Quân cờ, thời gian, highlight)
        drawGame(g2);

        // KIỂM TRA KẾT THÚC GAME
        if (controller.isGameOver()) {
            drawGameOver(g2);
        }
    }

    public Piece getPieceAt(int col, int row) {
        for (Piece p : controller.getSimPieces()) {
            if (p.col == col && p.row == row) return p;
        }
        return null;
    }

    private void drawGame(Graphics2D g2) {
        Piece activeP = controller.getActiveP();
        ArrayList<Piece> simPieces = controller.getSimPieces();

        // --- 1. Vẽ vùng highlight quân đang chọn và nước đi ---
        if (activeP != null) {
            // Vẽ ô vuông màu cam cho quân đang được chọn
            g2.setColor(new Color(255, 165, 0, 150));
            g2.fillRect(activeP.preCol * Board.SQUARE_SIZE, activeP.preRow * Board.SQUARE_SIZE,
                    Board.SQUARE_SIZE, Board.SQUARE_SIZE);

            if (controller.isClickedToMove()) {
                ArrayList<int[]> validMoves = controller.getValidMoves();
                for (int[] move : validMoves) {
                    int col = move[0];
                    int row = move[1];
                    int type = move[2]; // 0: Thường, 1: Tấn công

                    if (type == 0) {
                        g2.setColor(new Color(0, 255, 0, 100)); // Xanh cho ô trống
                        g2.fillRect(col * Board.SQUARE_SIZE, row * Board.SQUARE_SIZE, Board.SQUARE_SIZE, Board.SQUARE_SIZE);
                    } else {
                        // Logic Tấn công (Đỏ)
                        // Đặc biệt cho En Passant: Tô xanh ô đích, đỏ ô quân bị bắt
                        if (activeP.type == Type.PAWN && col != activeP.col && getPieceAt(col, row) == null) {
                            g2.setColor(new Color(0, 255, 0, 100)); // Ô đích xanh
                            g2.fillRect(col * Board.SQUARE_SIZE, row * Board.SQUARE_SIZE, Board.SQUARE_SIZE, Board.SQUARE_SIZE);

                            g2.setColor(new Color(255, 0, 0, 100)); // Quân bị bắt đỏ
                            g2.fillRect(col * Board.SQUARE_SIZE, activeP.row * Board.SQUARE_SIZE, Board.SQUARE_SIZE, Board.SQUARE_SIZE);
                        } else {
                            g2.setColor(new Color(255, 0, 0, 100)); // Bắt quân thường
                            g2.fillRect(col * Board.SQUARE_SIZE, row * Board.SQUARE_SIZE, Board.SQUARE_SIZE, Board.SQUARE_SIZE);
                        }
                    }
                }
            }
        }

        // --- 2. Vẽ tất cả các quân cờ ---
        for (Piece p : simPieces) {
            p.draw(g2);
        }

        // --- 3. Vẽ Thời gian ---
        if (GameState.currentState == State.PLAYING || GameState.currentState == State.PAUSED) {
            g2.setColor(Color.BLUE);
            g2.setFont(new Font("Arial", Font.BOLD, 30));
            g2.drawString("Time left: " + controller.getTimeLeft(), Board.BOARD_WIDTH + 20, 400);
        }

        // --- 4. Vẽ Trạng thái Check / Turn ---
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        int textX = Board.BOARD_WIDTH + 20;

        if (controller.isPromotion()) {
            g2.drawString("Promote to: ", textX, 150);
            for (Piece piece : controller.getPromoPieces()) {
                g2.drawImage(piece.image, piece.getX(piece.col), piece.getY(piece.row),
                        Board.SQUARE_SIZE, Board.SQUARE_SIZE, null);
            }
        } else {
            int currentColor = controller.getCurrentColor();
            Piece checkingP = controller.getCheckingP();

            if (checkingP != null) {
                // Lấy Vua của phe hiện tại (đang bị chiếu)
                Piece king = controller.getKing(false);

                if (king != null) {
                    // Vẽ highlight ĐỎ cho ô của Vua
                    g2.setColor(new Color(255, 0, 0, 150));
                    g2.fillRect(king.col * Board.SQUARE_SIZE, king.row * Board.SQUARE_SIZE,
                            Board.SQUARE_SIZE, Board.SQUARE_SIZE);

                    // Vẽ text thông báo trạng thái
                    g2.setColor(Color.RED);
                    g2.setFont(new Font("Arial", Font.BOLD, 20));
                    String kingColor = (king.color == GameController.WHITE) ? "White" : "Black";
                    g2.drawString(kingColor + " King is in check!", textX, king.color == GameController.WHITE ? 100 : 600);
                }
            }

            g2.setColor(Color.WHITE);
            g2.drawString(currentColor == GameController.WHITE ? "White turn" : "Black turn",
                    textX, currentColor == GameController.WHITE ? 700 : 100);
        }

        // Vẽ quân đang kéo (nếu có) vẽ sau cùng để nằm trên cùng
        if (activeP != null) activeP.draw(g2);
    }

    private void drawGameOver(Graphics2D g2) {
        // Vẽ lớp phủ mờ
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // ƯU TIÊN KIỂM TRA HÒA TRƯỚC
        if (controller.isDraw()) {
            g2.setFont(new Font("Arial", Font.BOLD, 70));
            g2.setColor(Color.YELLOW); // Màu vàng cho kết quả hòa
            String msg = "DRAW: INSUFFICIENT MATERIAL";
            g2.drawString(msg, getXforCenteredText(msg, g2), HEIGHT / 2);
        }
        // NẾU KHÔNG HÒA MỚI TÍNH ĐẾN THẮNG/THUA
        else {
            g2.setFont(new Font("Arial", Font.BOLD, 80));
            g2.setColor(Color.GREEN);

            // Logic xác định người thắng: Nếu đang là lượt Trắng mà bị chiếu hết -> Đen thắng
            String winner = (controller.getCurrentColor() == GameController.WHITE) ? "BLACK" : "WHITE";
            String msg = winner + " WINS!";
            g2.drawString(msg, getXforCenteredText(msg, g2), HEIGHT / 2);
        }

        g2.setFont(new Font("Arial", Font.PLAIN, 20));
        g2.setColor(Color.WHITE);
        g2.drawString("Press ESC to return to Menu", getXforCenteredText("Press ESC to return to Menu", g2), HEIGHT / 2 + 80);
    }

    // Hàm hỗ trợ căn giữa văn bản
    private int getXforCenteredText(String text, Graphics2D g2) {
        int length = (int) g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        return WIDTH / 2 - length / 2;
    }

    public BufferedImage getGameSnapshot() {
        BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        printAll(g2d); // Chụp cả các component như nút Pause
        g2d.dispose();
        return image;
    }
}