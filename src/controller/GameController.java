package controller;

import java.util.ArrayList;

import view.GamePanel;

import model.Bishop;
import model.Board;
import model.King;
import model.Knight;
import model.Pawn;
import model.Piece;
import model.Queen;
import model.Rook;
import model.Type;

public class GameController implements Runnable {

    //  king's movements
    private static final int[][] DIRECTIONS = {
            { -1, -1 }, { -1, 0 }, { -1, 1 },
            { 0, -1 },             { 0, 1 },
            { 1, -1 },  { 1, 0 },  { 1, 1 }
    };

    //  --- GAME STATE VARIABLES (MODEL LOGIC) ---
    public static final int WHITE = 0;
    public static final int BLACK = 1;
    private int currentColor = WHITE;

    private boolean promotion;
    private boolean gameOver;
    private boolean isTimeRunning = false;
    private boolean isClickedToMove = false;
    private boolean isCastling = false;
    private boolean isDraw = false;

    private long lastSecond = System.currentTimeMillis();
    private int timeLeft = 10;

    //  --- GAME COMPONENTS ---
    private GamePanel gamePanel; // Reference to the View
    private Thread gameThread;
    private final Board board = new Board(); // Board data/image loading
    public Mouse mouse = new Mouse(); // Mouse input handler

    //  --- PIECES ---
    private final ArrayList<Piece> pieces = new ArrayList<>();          // "real" position list
    public static ArrayList<Piece> simPieces = new ArrayList<>();      // simulation list used for move checks
    private final ArrayList<Piece> promoPieces = new ArrayList<>();
    private Piece activeP, checkingP;
    public static Piece castlingP;
    private final ArrayList<int[]> validMoves = new ArrayList<>();

    // --- CONSTRUCTOR ---
    public GameController() {
        setPieces();
        copyPieces(pieces, simPieces);

        // Kiểm tra trạng thái hòa/khởi đầu
        if (isInsufficientMaterial() || isStaleMate()) {
            isDraw = true;
            gameOver = true;
            isTimeRunning = false;
        } else {
            isTimeRunning = true;
        }
    }

    // --- PUBLIC INTERFACE FOR MAIN CLASS ---
    public void launchGame(GamePanel gp) {
        this.gamePanel = gp;
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void handleMouseRelease(int x, int y) {
        mouse.released = true;
        mouse.x = x;
        mouse.y = y;
    }

    // --- GETTERS FOR VIEW (GamePanel) ---
    public int getCurrentColor() { return currentColor; }
    public ArrayList<Piece> getSimPieces() { return simPieces; }
    public Board getBoard() { return board; }
    public Piece getActiveP() { return activeP; }
    public boolean isPromotion() { return promotion; }
    public ArrayList<Piece> getPromoPieces() { return promoPieces; }
    public Piece getCheckingP() { return checkingP; }
    public boolean isGameOver() { return gameOver; }
    public boolean isDraw() { return isDraw; }
    public int getTimeLeft() { return timeLeft; }
    public boolean isClickedToMove() { return isClickedToMove; }
    public ArrayList<int[]> getValidMoves() { return validMoves; }

    // --- TEST AREA (giữ nguyên các test thế cờ của bạn) ---
    public void testInsufficientMaterialDraw() {
        pieces.clear();
        // White
        pieces.add(new King(WHITE, 7, 6));   // Vua G1
        pieces.add(new Bishop(WHITE, 7, 5)); // Tượng F1 (sáng)
        // Black
        pieces.add(new King(BLACK, 0, 6));   // Vua G8
        pieces.add(new Bishop(BLACK, 0, 5)); // Tượng F8 (sáng)
        currentColor = WHITE;
        copyPieces(pieces, simPieces);
    }

    public void testCustomCheckmate() {
        pieces.clear();
        pieces.add(new Rook(WHITE, 7, 1));
        pieces.add(new King(WHITE, 7, 6));
        pieces.add(new Pawn(WHITE, 6, 5));
        pieces.add(new Pawn(WHITE, 6, 6));
        pieces.add(new Pawn(WHITE, 6, 7));
        pieces.add(new King(BLACK, 0, 6));
        pieces.add(new Pawn(BLACK, 1, 5));
        pieces.add(new Pawn(BLACK, 1, 6));
        pieces.add(new Pawn(BLACK, 1, 7));
        currentColor = WHITE;
        copyPieces(pieces, simPieces);
    }

    public void testAnastasiasMate() {
        pieces.clear();
        pieces.add(new Knight(WHITE, 1, 4));
        pieces.add(new Rook(WHITE, 5, 4));
        pieces.add(new King(WHITE, 7, 6));
        pieces.add(new King(BLACK, 1, 7));
        pieces.add(new Pawn(BLACK, 1, 6));
        currentColor = WHITE;
        copyPieces(pieces, simPieces);
    }

    public void testBishopCheckmate1() {
        pieces.clear();
        pieces.add(new King(WHITE, 7, 6));
        pieces.add(new Bishop(WHITE, 5, 6));
        pieces.add(new King(BLACK, 0, 7));
        pieces.add(new Pawn(BLACK, 1, 7));
        pieces.add(new Bishop(BLACK, 0, 6));
        currentColor = WHITE;
        copyPieces(pieces, simPieces);
    }

    public void testKnightCheckmate() {
        pieces.clear();
        pieces.add(new King(WHITE, 7, 2));
        pieces.add(new Rook(WHITE, 7, 6));
        pieces.add(new Knight(WHITE, 3, 4));
        pieces.add(new King(BLACK, 0, 7));
        pieces.add(new Pawn(BLACK, 1, 7));
        currentColor = WHITE;
        copyPieces(pieces, simPieces);
    }

    public void testDoubleCheckmate() {
        pieces.clear();
        pieces.add(new King(WHITE, 7, 2));
        pieces.add(new Bishop(WHITE, 4, 5));
        pieces.add(new Bishop(WHITE, 5, 3));
        pieces.add(new King(BLACK, 0, 2));
        pieces.add(new Pawn(BLACK, 1, 3));
        pieces.add(new Rook(BLACK, 0, 3));
        currentColor = WHITE;
        copyPieces(pieces, simPieces);
    }

    // --- GAME LOOP ---
    @Override
    public void run() {
        int FPS = 60;
        double drawInterval = (double) 1000000000 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while (gameThread != null) {
            currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
                update();
                if (gamePanel != null) gamePanel.repaint();
                delta--;
            }
        }
    }

    private void update() {
        long currentTimeMillis = System.currentTimeMillis();

        if (!gameOver) {
            if (isInsufficientMaterial() || isStaleMate()) {
                isDraw = true;
                gameOver = true;
                isTimeRunning = false;
                if (gamePanel != null) gamePanel.repaint();
                return;
            }
        }

        if (gameOver) return;

        // Thời gian
        if (currentTimeMillis - lastSecond >= 1000 && isTimeRunning) {
            lastSecond = currentTimeMillis;
            timeLeft--;

            // Nếu vua đang bị chiếu và là chiếu hết, game over
            if (isCheckMate() && isKingInCheck()) {
                gameOver = true;
                isTimeRunning = false;
            }

            if (timeLeft <= 0 && !promotion) {
                // xử lý hết giờ: bên hiện tại thua nếu vua đang bị chiếu - theo nhiều luật cược/giải có thể khác
                if (isKingInCheck()) {
                    gameOver = true;
                    isTimeRunning = false;
                } else {
                    // nếu không trong chiếu, bên đối thủ thắng vì bạn hết giờ
                    currentColor = (currentColor == WHITE) ? BLACK : WHITE;
                    resetTime();
                    isTimeRunning = true;
                }
            }
        }

        if (promotion) {
            promoting();
        } else {
            // Click-to-click logic
            if (mouse.released) {
                int col = mouse.x / Board.SQUARE_SIZE;
                int row = mouse.y / Board.SQUARE_SIZE;

                if (activeP == null) {
                    // CHỌN QUÂN (click 1)
                    for (Piece piece : simPieces) {
                        if (piece.color == currentColor && piece.col == col && piece.row == row) {
                            activeP = piece;
                            calculateValidMoves(activeP);
                            isClickedToMove = true;
                            break;
                        }
                    }
                } else {
                    // DI CHUYỂN HOẶC CHỌN QUÂN KHÁC (click 2)
                    boolean isMove = false;
                    for (int[] mv : validMoves) {
                        if (mv[0] == col && mv[1] == row) {
                            isMove = true;
                            break;
                        }
                    }

                    if (isMove) {
                        // Thực hiện nước đi trên simPieces rồi copy về pieces
                        simulateClickToMove(col, row);
                        copyPieces(simPieces, pieces);

                        activeP.updatePosition();
                        if (castlingP != null) castlingP.updatePosition();

                        // kiểm tra kết thúc
                        if (isKingInCheck() && isCheckMate()) {
                            gameOver = true;
                        }

                        if (isStaleMate()) {
                            isDraw = true;
                            gameOver = true;
                            isTimeRunning = false;
                        } else {
                            if (canPromote()) {
                                promotion = true;
                            } else {
                                isClickedToMove = false;
                                activeP = null;
                                validMoves.clear();
                                changePlayer();
                                resetTime();
                                isTimeRunning = true;
                            }
                        }
                    } else if (activeP.col == col && activeP.row == row) {
                        // huỷ chọn
                        activeP = null;
                        isClickedToMove = false;
                        validMoves.clear();
                    } else {
                        // chọn quân khác cùng màu
                        Piece newPiece = null;
                        for (Piece piece : simPieces) {
                            if (piece.color == currentColor && piece.col == col && piece.row == row) {
                                newPiece = piece;
                                break;
                            }
                        }
                        if (newPiece != null) {
                            activeP = newPiece;
                            calculateValidMoves(activeP);
                        } else {
                            // click vào ô trống / đối phương -> huỷ chọn
                            activeP = null;
                            isClickedToMove = false;
                            validMoves.clear();
                        }
                    }
                }

                mouse.released = false;
            }
        }
    }

    // --- BASIC SETUP HELPERS ---
    public void setPieces() {
        pieces.clear();
        // white
        for (int c = 0; c < 8; c++) pieces.add(new Pawn(WHITE, 6, c));
        pieces.add(new Rook(WHITE, 7, 0));
        pieces.add(new Rook(WHITE, 7, 7));
        pieces.add(new Knight(WHITE, 7, 1));
        pieces.add(new Knight(WHITE, 7, 6));
        pieces.add(new Bishop(WHITE, 7, 2));
        pieces.add(new Bishop(WHITE, 7, 5));
        pieces.add(new King(WHITE, 7, 4));
        pieces.add(new Queen(WHITE, 7, 3));

        // black
        for (int c = 0; c < 8; c++) pieces.add(new Pawn(BLACK, 1, c));
        pieces.add(new Rook(BLACK, 0, 0));
        pieces.add(new Rook(BLACK, 0, 7));
        pieces.add(new Knight(BLACK, 0, 1));
        pieces.add(new Knight(BLACK, 0, 6));
        pieces.add(new Bishop(BLACK, 0, 2));
        pieces.add(new Bishop(BLACK, 0, 5));
        pieces.add(new King(BLACK, 0, 4));
        pieces.add(new Queen(BLACK, 0, 3));
    }

    /**
     * Copy references from src to tgt (shallow copy of list).
     * Nếu bạn muốn clone object quân cờ, cần implement clone trong Piece.
     */
    public void copyPieces(ArrayList<Piece> src, ArrayList<Piece> tgt) {
        tgt.clear();
        for (Piece piece : src) {
            tgt.add(piece);
        }
    }

    public void resetTime() {
        timeLeft = 10;
        lastSecond = System.currentTimeMillis();
    }

    // --- VALID MOVE CALCULATION ---
    private void calculateValidMoves(Piece p) {
        validMoves.clear();
        if (p == null) return;
        if (!simPieces.contains(p)) {
            // đảm bảo p là object trong simPieces
            return;
        }

        for (int tr = 0; tr < 8; tr++) {
            for (int tc = 0; tc < 8; tc++) {
                if (!p.canMove(tc, tr)) continue;

                // Simulate: nếu sau nước đi vua an toàn -> hợp lệ
                if (simulateMoveAndKingSafe(p, tc, tr)) {
                    validMoves.add(new int[] { tc, tr });
                }
            }
        }
    }

    /**
     * Thực hiện mô phỏng nước đi (thay đổi trên simPieces), kiểm tra xem sau nước đi,
     * vua của màu đang đi có an toàn (không bị đối phương ăn) hay không.
     * Hàm này sẽ phục hồi trạng thái simPieces/vi trí quân cờ sau khi kiểm tra.
     */
    private boolean simulateMoveAndKingSafe(Piece piece, int targetCol, int targetRow) {
        // Lưu trạng thái cũ
        int oldRow = piece.row;
        int oldCol = piece.col;

        Piece captured = piece.gettingHitP(targetCol, targetRow);
        boolean removed = false;
        if (captured != null) {
            removed = simPieces.remove(captured);
        }

        // Di chuyển
        piece.col = targetCol;
        piece.row = targetRow;

        boolean kingSafe = !opponentsCanCaptureKing();

        // Phục hồi
        piece.col = oldCol;
        piece.row = oldRow;
        if (removed && captured != null) {
            simPieces.add(captured);
        }

        return kingSafe;
    }

    // --- SIMULATE CLICK-TO-MOVE (khi người chơi xác nhận nước đi) ---
    public void simulateClickToMove(int targetCol, int targetRow) {
        // Reset simPieces từ pieces (đảm bảo luôn đúng trạng thái bắt đầu)
        copyPieces(pieces, simPieces);

        // Reset castling rook nếu cần (đảm bảo đồng nhất vị trí ban đầu)
        if (castlingP != null) {
            castlingP.col = castlingP.preCol;
            castlingP.x = castlingP.getX(castlingP.col);
            castlingP = null;
        }

        // Xác định hittingP và castlingP thông qua canMove
        activeP.canMove(targetCol, targetRow);

        // Nếu có quân bị bắt, loại khỏi simPieces
        if (activeP.hittingP != null) {
            simPieces.remove(activeP.hittingP);
        }

        // Cập nhật vị trí cuối cùng
        activeP.col = targetCol;
        activeP.row = targetRow;

        // Xử lý castling nếu có
        checkCastling();
    }

    private void checkCastling() {
        if (castlingP != null) {
            if (castlingP.col == 0) castlingP.col += 3;
            if (castlingP.col == 7) castlingP.col -= 2;
            castlingP.x = castlingP.getX(castlingP.col);
            isCastling = true;
        }
    }

    private boolean isIllegal(Piece currentP) {
        // Kiểm tra xem vị trí vua có bị ăn bởi đối phương hay không
        if (currentP.type == Type.KING) {
            for (Piece piece : simPieces) {
                if (piece.color != currentP.color && piece != currentP && piece.canMove(currentP.col, currentP.row)) {
                    return true;
                }
            }
        }
        return false;
    }

    // --- GAME ENDING CONDITIONS ---

    /**
     * Stalemate: bên đang đi không trong chiếu, nhưng không có nước hợp lệ nào.
     */
    private boolean isStaleMate() {
        if (isKingInCheck()) return false;

        for (Piece piece : simPieces) {
            if (piece.color != currentColor) continue;
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    if (piece.canMove(c, r) && simulateMoveAndKingSafe(piece, c, r)) {
                        return false; // có ít nhất một nước hợp lệ
                    }
                }
            }
        }
        return true; // không có nước hợp lệ nào
    }

    /**
     * Checkmate: vua đang bị chiếu và không có bất kỳ nước hợp lệ nào để phá chiếu.
     * Triển khai tổng quát bằng cách thử mọi nước đi của bên đang bị chiếu.
     */
    private boolean isCheckMate() {
        if (!isKingInCheck()) return false;

        for (Piece piece : simPieces) {
            if (piece.color != currentColor) continue;
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    if (piece.canMove(c, r) && simulateMoveAndKingSafe(piece, c, r)) {
                        return false; // có nước phá chiếu
                    }
                }
            }
        }
        return true; // không có nước phá chiếu -> chiếu hết
    }

    // --- KING / CHECK HELPERS ---
    private boolean opponentsCanCaptureKing() {
        Piece king = getKing(false);
        if (king == null) return false;
        for (Piece piece : simPieces) {
            if (piece.color != king.color && piece.canMove(king.col, king.row)) {
                return true;
            }
        }
        return false;
    }

    private boolean isKingInCheck() {
        Piece king = getKing(false);
        if (king == null) return false;
        for (Piece piece : simPieces) {
            if (piece.color != king.color && piece.canMove(king.col, king.row)) {
                checkingP = piece;
                return true;
            }
        }
        checkingP = null;
        return false;
    }

    /**
     * getKing(false) => trả về Vua của bên đang đi (currentColor)
     * getKing(true)  => trả về Vua đối phương
     */
    public Piece getKing(boolean opponent) {
        for (Piece piece : simPieces) {
            if (piece.type == Type.KING) {
                if (opponent) {
                    if (piece.color != currentColor) return piece;
                } else {
                    if (piece.color == currentColor) return piece;
                }
            }
        }
        return null;
    }

    // --- PROMOTION ---
    private boolean canPromote() {
        if (activeP == null) return false;
        if (activeP.type == Type.PAWN) {
            if ((currentColor == WHITE && activeP.row == 0) || (currentColor == BLACK && activeP.row == 7)) {
                promoPieces.clear();

                long rookCount = simPieces.stream().filter(p -> p instanceof Rook && p.color == currentColor).count();
                if (rookCount <= 1) promoPieces.add(new Rook(currentColor, 4, 9));

                long queenCount = simPieces.stream().filter(p -> p instanceof Queen && p.color == currentColor).count();
                if (queenCount == 0) promoPieces.add(new Queen(currentColor, 2, 9));

                long bishopCount = simPieces.stream().filter(p -> p instanceof Bishop && p.color == currentColor).count();
                if (bishopCount <= 1) promoPieces.add(new Bishop(currentColor, 5, 9));

                long knightCount = simPieces.stream().filter(p -> p instanceof Knight && p.color == currentColor).count();
                if (knightCount <= 1) promoPieces.add(new Knight(currentColor, 3, 9));

                return !promoPieces.isEmpty();
            }
        }
        return false;
    }

    private void promoting() {
        if (!mouse.released) return;

        int selCol = mouse.x / Board.SQUARE_SIZE;
        int selRow = mouse.y / Board.SQUARE_SIZE;

        for (Piece piece : promoPieces) {
            if (piece.col == selCol && piece.row == selRow) {
                // Thực hiện thăng cấp tương ứng
                switch (piece.type) {
                    case ROOK:
                        if (simPieces.stream().filter(p -> p instanceof Rook && p.color == currentColor).count() <= 1) {
                            simPieces.add(new Rook(currentColor, activeP.row, activeP.col));
                        }
                        break;
                    case QUEEN:
                        if (simPieces.stream().filter(p -> p instanceof Queen && p.color == currentColor).count() == 0) {
                            simPieces.add(new Queen(currentColor, activeP.row, activeP.col));
                        }
                        break;
                    case BISHOP:
                        if (simPieces.stream().filter(p -> p instanceof Bishop && p.color == currentColor).count() <= 1) {
                            simPieces.add(new Bishop(currentColor, activeP.row, activeP.col));
                        }
                        break;
                    case KNIGHT:
                        if (simPieces.stream().filter(p -> p instanceof Knight && p.color == currentColor).count() <= 1) {
                            simPieces.add(new Knight(currentColor, activeP.row, activeP.col));
                        }
                        break;
                    default:
                        break;
                }
                simPieces.remove(activeP);
                copyPieces(simPieces, pieces);
                activeP = null;
                promotion = false;
                changePlayer();
                resetTime();
                mouse.released = false;
                break;
            }
        }
    }

    // --- TURN CHANGE ---
    private void changePlayer() {
        if (mouse.pressed && activeP != null) {
            activeP.resetPosition();
        }

        if (isCastling && mouse.pressed && castlingP != null) {
            castlingP.resetPosition();
            if (activeP != null) activeP.resetPosition();
        }

        currentColor = (currentColor == WHITE) ? BLACK : WHITE;
        isKingInCheck(); // cập nhật checkingP
        for (Piece piece : pieces) {
            if (piece.color == currentColor) piece.twoStepped = false;
        }

        activeP = null;
    }

    /**
     * Kiểm tra hòa do thiếu vật chất:
     * - K vs K
     * - K vs K+N
     * - K vs K+B
     * - K+B vs K+B (2 bishops trên cùng màu ô)
     * Các trường hợp khác không tính là hòa do thiếu vật chất.
     */
    private boolean isInsufficientMaterial() {
        // Sao chép trạng thái hiện tại
        ArrayList<Piece> list = simPieces;
        // Nếu có pawn, rook, queen => không thiếu vật chất
        for (Piece p : list) {
            if (p.type == Type.PAWN || p.type == Type.ROOK || p.type == Type.QUEEN) return false;
        }

        int total = list.size();
        if (total == 2) {
            // chỉ còn 2 vua
            return true;
        }
        if (total == 3) {
            // K + (N or B) vs K
            // kiểm tra có đúng 1 minor piece (bishop hoặc knight) + 2 kings
            int minor = 0;
            for (Piece p : list) {
                if (p.type == Type.BISHOP || p.type == Type.KNIGHT) minor++;
            }
            return minor == 1;
        }
        if (total == 4) {
            // K+B vs K+B -> chỉ hòa khi 2 bishop cùng màu ô
            Piece wB = null, bB = null;
            int otherCount = 0;
            for (Piece p : list) {
                if (p.type == Type.BISHOP) {
                    if (p.color == WHITE) wB = p;
                    else bB = p;
                } else if (p.type != Type.KING) {
                    otherCount++;
                }
            }
            if (wB != null && bB != null && otherCount == 0) {
                int color1 = (wB.row + wB.col) % 2;
                int color2 = (bB.row + bB.col) % 2;
                return color1 == color2;
            }
        }
        return false; // Các trường hợp khác không được tính là hòa do thiếu vật chất
    }
}
