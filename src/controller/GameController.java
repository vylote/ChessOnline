package controller;

import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.io.*;

import model.*;
import view.GamePanel;
import view.MenuPauseFrame;
import javax.swing.*;

public class GameController implements Runnable {
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
    private GamePanel gamePanel;
    private Thread gameThread;
    private final Board board = new Board();
    public Mouse mouse = new Mouse();

    //  --- PIECES ---
    private final ArrayList<Piece> pieces = new ArrayList<>();
    public static ArrayList<Piece> simPieces = new ArrayList<>();
    private final ArrayList<Piece> promoPieces = new ArrayList<>();
    private Piece activeP, checkingP;
    public static Piece castlingP;
    private final ArrayList<int[]> validMoves = new ArrayList<>();

    // --- FRAMES ---
    private JFrame mainFrame;
    private JFrame uiFrame;

    public GameController() {
        GameState.setState(State.MENU);
        setPieces();
        copyPieces(pieces, simPieces);
    }

    public void setMainFrame(JFrame frame) { this.mainFrame = frame; }
    public void setUiFrame(JFrame frame) { this.uiFrame = frame; }

    // --- GAME CONTROL METHODS ---

    public void exitToMenu() {
        GameState.setState(State.MENU);
        isTimeRunning = false;
        if (mainFrame != null) mainFrame.setVisible(false);

        MenuPauseFrame newUiFrame = new MenuPauseFrame(this);
        this.uiFrame = newUiFrame;
        newUiFrame.setVisible(true);
    }

    public void startNewGame() {
        setPieces();
        copyPieces(pieces, simPieces);

        currentColor = WHITE;
        isDraw = false;
        gameOver = false;
        isClickedToMove = false;
        promotion = false;
        resetTime();

        if (isInsufficientMaterial() || isStaleMate()) {
            isDraw = true;
            gameOver = true;
            isTimeRunning = false;
        } else {
            isTimeRunning = true;
        }

        GameState.setState(State.PLAYING);
        if (uiFrame != null) uiFrame.dispose();
        if (mainFrame != null) mainFrame.setVisible(true);
    }

    public void resumeGame() {
        if (GameState.currentState == State.PAUSED) {
            isTimeRunning = true;
            GameState.setState(State.PLAYING);
            if (uiFrame != null) uiFrame.dispose();
            if (mainFrame != null) mainFrame.setVisible(true);
        }
    }

    private BufferedImage gameSnapshot;
    public void pauseGame() {
        if (GameState.currentState == State.PLAYING) {
            isTimeRunning = false;
            GameState.setState(State.PAUSED);
            SwingUtilities.invokeLater(() -> {
                if (gamePanel != null) {
                    this.gameSnapshot = gamePanel.getGameSnapshot();
                }
                if (mainFrame != null) mainFrame.setVisible(false);
                if (uiFrame != null) {
                    ((MenuPauseFrame)uiFrame).setPausePanel(this.gameSnapshot);
                    uiFrame.setVisible(true);
                }
            });
        }
    }

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

    // --- GETTERS ---
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
    public boolean isTimeRunning() { return isTimeRunning; }

    @Override
    public void run() {
        int FPS = 60;
        double drawInterval = (double) 1000000000 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();

        while (gameThread != null) {
            long currentTime = System.nanoTime();
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
        if (GameState.currentState != State.PLAYING || gameOver) return;

        long currentTimeMillis = System.currentTimeMillis();

        // Kiểm tra hòa ngay lập tức
        if (isInsufficientMaterial() || isStaleMate()) {
            isDraw = true;
            gameOver = true;
            isTimeRunning = false;
            return;
        }

        // Cập nhật đồng hồ
        if (currentTimeMillis - lastSecond >= 1000 && isTimeRunning) {
            lastSecond = currentTimeMillis;
            timeLeft--;

            if (timeLeft <= 0 && !promotion) {
                if (isKingInCheck()) {
                    gameOver = true;
                    isTimeRunning = false;
                } else {
                    currentColor = (currentColor == WHITE) ? BLACK : WHITE;
                    resetTime();
                }
            }
        }

        if (promotion) {
            promoting();
        } else {
            handleMouseInput();
        }
    }

    private void handleMouseInput() {
        if (mouse.released) {
            int col = mouse.x / Board.SQUARE_SIZE;
            int row = mouse.y / Board.SQUARE_SIZE;

            if (activeP == null) {
                for (Piece piece : simPieces) {
                    if (piece.color == currentColor && piece.col == col && piece.row == row) {
                        activeP = piece;
                        calculateValidMoves(activeP);
                        isClickedToMove = true;
                        break;
                    }
                }
            } else {
                boolean isMove = false;
                for (int[] mv : validMoves) {
                    if (mv[0] == col && mv[1] == row) {
                        isMove = true;
                        break;
                    }
                }

                if (isMove) {
                    simulateClickToMove(col, row);
                    copyPieces(simPieces, pieces);
                    activeP.updatePosition();
                    if (castlingP != null) castlingP.updatePosition();

                    if (isKingInCheck() && isCheckMate()) {
                        gameOver = true;
                        isTimeRunning = false;
                    } else if (isStaleMate()) {
                        isDraw = true;
                        gameOver = true;
                        isTimeRunning = false;
                    } else {
                        if (canPromote()) {
                            promotion = true;
                        } else {
                            finalizeTurn();
                        }
                    }
                } else {
                    cancelOrSwitchSelection(col, row);
                }
            }
            mouse.released = false;
        }
    }

    private void finalizeTurn() {
        isClickedToMove = false;
        activeP = null;
        validMoves.clear();
        changePlayer();
        resetTime();
        isTimeRunning = true;
    }

    private void cancelOrSwitchSelection(int col, int row) {
        if (activeP.col == col && activeP.row == row) {
            activeP = null;
            isClickedToMove = false;
            validMoves.clear();
        } else {
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
                activeP = null;
                isClickedToMove = false;
                validMoves.clear();
            }
        }
    }

    public void setPieces() {
        ChessSetupUtility.setupStandardGame(this.pieces);
    }

    public void copyPieces(ArrayList<Piece> src, ArrayList<Piece> tgt) {
        tgt.clear();
        tgt.addAll(src);
    }

    public void resetTime() {
        timeLeft = 10;
        lastSecond = System.currentTimeMillis();
    }

    private void calculateValidMoves(Piece p) {
        validMoves.clear();
        if (p == null || !simPieces.contains(p)) return;

        for (int tr = 0; tr < 8; tr++) {
            for (int tc = 0; tc < 8; tc++) {
                if (p.canMove(tc, tr)) {
                    if (simulateMoveAndKingSafe(p, tc, tr)) {
                        int type = (p.gettingHitP(tc, tr) != null || (p.type == Type.PAWN && tc != p.col)) ? 1 : 0;
                        validMoves.add(new int[] { tc, tr, type });
                    }
                }
            }
        }
    }

    private boolean simulateMoveAndKingSafe(Piece piece, int targetCol, int targetRow) {
        int oldRow = piece.row;
        int oldCol = piece.col;
        Piece captured = piece.gettingHitP(targetCol, targetRow);

        if (captured != null) simPieces.remove(captured);
        piece.col = targetCol;
        piece.row = targetRow;

        boolean kingSafe = !opponentsCanCaptureKing();

        piece.col = oldCol;
        piece.row = oldRow;
        if (captured != null) simPieces.add(captured);

        return kingSafe;
    }

    public void simulateClickToMove(int targetCol, int targetRow) {
        copyPieces(pieces, simPieces);
        if (castlingP != null) {
            castlingP.col = castlingP.preCol;
            castlingP.x = castlingP.getX(castlingP.col);
            castlingP = null;
        }
        activeP.canMove(targetCol, targetRow);
        if (activeP.hittingP != null) simPieces.remove(activeP.hittingP);
        activeP.col = targetCol;
        activeP.row = targetRow;
        checkCastling();
    }

    private void checkCastling() {
        if (castlingP != null) {
            if (castlingP.col == 0) castlingP.col += 3;
            else if (castlingP.col == 7) castlingP.col -= 2;
            castlingP.x = castlingP.getX(castlingP.col);
            isCastling = true;
        }
    }

    private boolean isCheckMate() {
        if (!isKingInCheck()) return false;
        for (int i = 0; i < simPieces.size(); i++) {
            Piece piece = simPieces.get(i);
            if (piece.color != currentColor) continue;
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    if (piece.canMove(c, r) && simulateMoveAndKingSafe(piece, c, r)) return false;
                }
            }
        }
        return true;
    }

    private boolean isStaleMate() {
        if (isKingInCheck()) return false;
        for (int i = 0; i < simPieces.size(); i++) {
            Piece piece = simPieces.get(i);
            if (piece.color != currentColor) continue;
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    if (piece.canMove(c, r) && simulateMoveAndKingSafe(piece, c, r)) return false;
                }
            }
        }
        return true;
    }

    private boolean opponentsCanCaptureKing() {
        Piece king = getKing(false);
        if (king == null) return false;
        for (int i = 0; i < simPieces.size(); i++) {
            Piece piece = simPieces.get(i);
            if (piece.color != king.color && piece.canMove(king.col, king.row)) return true;
        }
        return false;
    }

    public boolean isKingInCheck() {
        Piece king = getKing(false);
        if (king == null) return false;
        for (int i = 0; i < simPieces.size(); i++) {
            Piece piece = simPieces.get(i);
            if (piece.color != king.color && piece.canMove(king.col, king.row)) {
                checkingP = piece;
                return true;
            }
        }
        checkingP = null;
        return false;
    }

    public Piece getKing(boolean opponent) {
        int colorToFind = opponent ? (currentColor == WHITE ? BLACK : WHITE) : currentColor;
        for (Piece piece : simPieces) {
            if (piece.type == Type.KING && piece.color == colorToFind) return piece;
        }
        return null;
    }

    private boolean canPromote() {
        if (activeP == null || activeP.type != Type.PAWN) return false;
        return (currentColor == WHITE && activeP.row == 0) || (currentColor == BLACK && activeP.row == 7);
    }

    private void promoting() {
        if (!mouse.released) return;
        int selCol = mouse.x / Board.SQUARE_SIZE;
        int selRow = mouse.y / Board.SQUARE_SIZE;

        for (Piece p : promoPieces) {
            if (p.col == selCol && p.row == selRow) {
                Piece newP = null;
                if (p.type == Type.ROOK) newP = new Rook(currentColor, activeP.col, activeP.row);
                if (p.type == Type.QUEEN) newP = new Queen(currentColor, activeP.col, activeP.row);
                if (p.type == Type.BISHOP) newP = new Bishop(currentColor, activeP.col, activeP.row);
                if (p.type == Type.KNIGHT) newP = new Knight(currentColor, activeP.col, activeP.row);

                if (newP != null) {
                    simPieces.remove(activeP);
                    simPieces.add(newP);
                    copyPieces(simPieces, pieces);
                    promotion = false;
                    finalizeTurn();
                }
                break;
            }
        }
    }

    private void changePlayer() {
        currentColor = (currentColor == WHITE) ? BLACK : WHITE;
        for (Piece piece : pieces) {
            if (piece.color == currentColor) piece.twoStepped = false;
        }
    }

    private boolean isInsufficientMaterial() {
        if (simPieces.size() > 4) return false;
        for (Piece p : simPieces) {
            if (p.type == Type.PAWN || p.type == Type.ROOK || p.type == Type.QUEEN) return false;
        }
        if (simPieces.size() == 2) return true; // K vs K
        if (simPieces.size() == 3) { // K + (N or B) vs K
            return simPieces.stream().anyMatch(p -> p.type == Type.BISHOP || p.type == Type.KNIGHT);
        }
        return false;
    }

    public void saveGame() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("savegame.dat"))) {
            // Tạo đối tượng lưu trữ
            SaveData data = new SaveData(this.pieces, this.currentColor, this.timeLeft);
            oos.writeObject(data);

            JOptionPane.showMessageDialog(null, "Game Saved Successfully!");
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error saving game!");
        }
    }

    public void loadGame() {
        File saveFile = new File("savegame.dat");
        if (!saveFile.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(saveFile))) {
            SaveData data = (SaveData) ois.readObject();

            this.pieces.clear();
            this.pieces.addAll(data.pieces);
            this.currentColor = data.currentColor;
            this.timeLeft = data.timeLeft;

            // NẠP LẠI ẢNH: Sử dụng đúng logic đường dẫn của bạn
            for (Piece p : this.pieces) {
                p.image = reloadPieceImage(p);
            }

            copyPieces(this.pieces, simPieces);
            GameState.setState(State.PLAYING);
            isTimeRunning = true;

            if (uiFrame != null) uiFrame.dispose();
            if (mainFrame != null) {
                mainFrame.setVisible(true);
                gamePanel.repaint();
            }
            JOptionPane.showMessageDialog(null, "Load thành công!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Hàm này mô phỏng lại logic trong Constructor của các quân cờ (King, Queen, Pawn...)
    private BufferedImage reloadPieceImage(Piece p) {
        String prefix = (p.color == WHITE) ? "w" : "b";
        String typeName = p.type.toString().toLowerCase();

        // Chuyển đổi typeName để khớp với tên file (Ví dụ: KING -> King)
        typeName = typeName.substring(0, 1).toUpperCase() + typeName.substring(1);

        // Đường dẫn phải khớp với hàm getImage("/piece/...") trong lớp King
        return p.getImage("/piece/" + prefix + typeName);
    }
}