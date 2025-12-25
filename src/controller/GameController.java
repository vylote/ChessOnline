package controller;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;

import model.*;
import utility.AudioManager;
import view.*;

/**
 * Controller trung tâm hoàn chỉnh: Sửa lỗi nhập thành overlap, Phong cấp thực tế và Save/Load.
 */
public class GameController implements Runnable {
    // --- GIAO DIỆN ---
    private final MainFrame window;
    private GamePanel gamePanel;
    private final MenuPanel menuPanel;
    private final PausePanel pausePanel;

    // --- TRẠNG THÁI ---
    public static final int WHITE = 0;
    public static final int BLACK = 1;
    private int currentColor = WHITE;

    private boolean promotion, gameOver, isDraw;
    private boolean isTimeRunning = false;
    private boolean isClickedToMove = false;
    private boolean isCastling = false;

    private long lastSecond = System.currentTimeMillis();
    private int timeLeft = 10;

    // --- THÀNH PHẦN LOGIC (An toàn đa luồng) ---
    private final Board board = new Board();
    public Mouse mouse = new Mouse();
    private final CopyOnWriteArrayList<Piece> pieces = new CopyOnWriteArrayList<>();
    public static CopyOnWriteArrayList<Piece> simPieces = new CopyOnWriteArrayList<>();

    private final ArrayList<Piece> promoPieces = new ArrayList<>();
    private Piece activeP, checkingP;
    public static Piece castlingP; // Quân Xe dùng để nhập thành
    private final ArrayList<int[]> validMoves = new ArrayList<>();

    // --- ÂM THANH ---
    private final AudioManager audioManager;
    private Thread gameThread;

    public GameController() {
        this.audioManager = new AudioManager();
        this.window = new MainFrame();
        this.window.getContentPane().setBackground(Color.BLACK);
        this.menuPanel = new MenuPanel(this, window);
        this.pausePanel = new PausePanel(this, window);

        showPanel(menuPanel);
        audioManager.playBGM("res/audio/bgm/menu_theme.wav");
        GameState.setState(State.MENU);

        setPieces();
        copyPieces(pieces, simPieces);
    }

    private void showPanel(JPanel panel) { window.showPanel(panel); }

    public void startNewGame() {
        setPieces();
        copyPieces(pieces, simPieces);
        currentColor = WHITE;
        isDraw = false; gameOver = false; isClickedToMove = false; promotion = false;
        castlingP = null; isCastling = false;
        resetTime();

        gamePanel = new GamePanel(this);
        showPanel(gamePanel);
        audioManager.playBGM("res/audio/bgm/game_theme.wav");

        if (isInsufficientMaterial() || isStaleMate()) {
            triggerEndGame(true, null);
        } else if (isKingInCheck() && isCheckMate()) {
            triggerEndGame(false, BLACK);
        } else {
            isTimeRunning = true;
            GameState.currentState = State.PLAYING;
        }

        if (gameThread == null || !gameThread.isAlive()) {
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    // --- ĐIỀU KHIỂN GIAO DIỆN (Sửa lỗi image_246b90, image_24cdc4, image_24e08a) ---
    public void pauseGame() {
        if (GameState.currentState == State.PLAYING) {
            isTimeRunning = false;
            GameState.setState(State.PAUSED);
            pausePanel.setBackgroundSnapshot(gamePanel.getGameSnapshot());
            pausePanel.loadAllThumbnails();
            showPanel(pausePanel);
        }
    }

    public void resumeGame() {
        if (GameState.currentState == State.PAUSED) {
            isTimeRunning = true;
            GameState.setState(State.PLAYING);
            showPanel(gamePanel);
        }
    }

    public void exitToMenu() {
        isTimeRunning = false;
        GameState.setState(State.MENU);
        menuPanel.resetMenu();
        audioManager.playBGM("res/audio/bgm/menu_theme.wav");
        showPanel(menuPanel);
    }

    public void handleMouseRelease(int x, int y) {
        mouse.released = true;
        mouse.x = x;
        mouse.y = y;
    }

    @Override
    public void run() {
        double drawInterval = 1000000000.0 / 60;
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
        isKingInCheck();

        if (isInsufficientMaterial()) { triggerEndGame(true, null); return; }

        long now = System.currentTimeMillis();
        if (isTimeRunning && now - lastSecond >= 1000) {
            lastSecond = now;
            timeLeft--;

            if (timeLeft <= 0) {
                if (promotion && !promoPieces.isEmpty()) {
                    replacePawnAndFinish(promoPieces.get(0));
                } else if (isKingInCheck()) {
                    triggerEndGame(false, (currentColor == WHITE ? BLACK : WHITE));
                } else {
                    finalizeTurn();
                }
                return;
            }
        }
        if (promotion) promoting(); else handleMouseInput();
    }

    private void handleMouseInput() {
        updateCursorState();
        if (!mouse.released) return;

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
            for (int[] mv : validMoves) if (mv[0] == col && mv[1] == row) { isMove = true; break; }

            if (isMove) {
                Piece captured = activeP.gettingHitP(col, row);
                simulateClickToMove(col, row);
                activeP.finishMove();

                // Đồng bộ preCol/preRow cho Xe nếu nhập thành để tránh lỗi nhảy nước đi sau đó
                if (isCastling && castlingP != null) castlingP.finishMove();

                copyPieces(simPieces, pieces);

                if (captured != null) audioManager.playSFX("res/audio/sfx/capture.wav");
                else if (isCastling) { audioManager.playSFX("res/audio/sfx/castle.wav"); isCastling = false; }
                else audioManager.playSFX("res/audio/sfx/move.wav");

                Timer sfxTimer = new Timer(200, e -> {
                    if (isKingInCheck() && !gameOver) audioManager.playSFX("res/audio/sfx/check.wav");
                    ((Timer)e.getSource()).stop();
                });
                sfxTimer.setRepeats(false); sfxTimer.start();

                if (isKingInCheck() && isCheckMate()) {
                    triggerEndGame(false, (currentColor == WHITE ? BLACK : WHITE));
                } else if (isStaleMate()) {
                    triggerEndGame(true, null);
                } else {
                    if (canPromote()) promotion = true; else finalizeTurn();
                }
            } else cancelOrSwitchSelection(col, row);
        }
        mouse.released = false;
    }

    /**
     * FIX LỖI NHẬP THÀNH OVERLAP: Gán chính xác cột đích và cập nhật pixel ngay lập tức.
     */
    public void simulateClickToMove(int tc, int tr) {
        copyPieces(pieces, simPieces);
        activeP.canMove(tc, tr);
        if (activeP.hittingP != null) simPieces.remove(activeP.hittingP);

        activeP.col = tc;
        activeP.row = tr;

        if (castlingP != null) {
            // Nhập thành trái (Queenside): Vua cột 2, Xe cột 3
            if (castlingP.col == 0) castlingP.col = 3;
                // Nhập thành phải (Kingside): Vua cột 6, Xe cột 5
            else if (castlingP.col == 7) castlingP.col = 5;

            castlingP.x = castlingP.getX(castlingP.col);
            castlingP.y = castlingP.getY(castlingP.row);
            isCastling = true;
        }
    }

    /**
     * PHONG CẤP THỰC TẾ: Đếm quân trên bàn cờ, chỉ phong cấp quân đang thiếu.
     */
    private boolean canPromote() {
        if (activeP == null || activeP.type != Type.PAWN) return false;
        if ((activeP.color == WHITE && activeP.row == 0) || (activeP.color == BLACK && activeP.row == 7)) {
            promoPieces.clear();
            int q = 0, r = 0, b = 0, n = 0;
            for (Piece p : simPieces) {
                if (p.color == currentColor) {
                    if (p.type == Type.QUEEN) q++;
                    else if (p.type == Type.ROOK) r++;
                    else if (p.type == Type.BISHOP) b++;
                    else if (p.type == Type.KNIGHT) n++;
                }
            }
            int displayRow = 7; // Sidebar hàng 7 kiểu Minecraft
            if (q < 1) addPromo(Type.QUEEN, displayRow);
            if (r < 2) addPromo(Type.ROOK, displayRow);
            if (b < 2) addPromo(Type.BISHOP, displayRow);
            if (n < 2) addPromo(Type.KNIGHT, displayRow);

            return !promoPieces.isEmpty();
        }
        return false;
    }

    private void addPromo(Type t, int r) {
        int c = 8 + promoPieces.size();
        Piece p = null;
        if (t == Type.QUEEN) p = new Queen(currentColor, r, c);
        else if (t == Type.ROOK) p = new Rook(currentColor, r, c);
        else if (t == Type.BISHOP) p = new Bishop(currentColor, r, c);
        else if (t == Type.KNIGHT) p = new Knight(currentColor, r, c);
        if (p != null) {
            p.image = reloadPieceImage(p);
            p.x = p.getX(p.col); p.y = p.getY(p.row);
            promoPieces.add(p);
        }
    }

    private void replacePawnAndFinish(Piece p) {
        Piece newP = null;
        if (p.type == Type.QUEEN) newP = new Queen(currentColor, activeP.row, activeP.col);
        else if (p.type == Type.ROOK) newP = new Rook(currentColor, activeP.row, activeP.col);
        else if (p.type == Type.BISHOP) newP = new Bishop(currentColor, activeP.row, activeP.col);
        else if (p.type == Type.KNIGHT) newP = new Knight(currentColor, activeP.row, activeP.col);

        if (newP != null) {
            newP.image = reloadPieceImage(newP); newP.updatePosition();
            simPieces.remove(activeP); simPieces.add(newP); copyPieces(simPieces, pieces);
        }
        audioManager.playSFX("res/audio/sfx/promote.wav");
        promotion = false; finalizeTurn();
    }

    private void promoting() {
        if (!mouse.released) return;
        for (Piece p : promoPieces) {
            if (mouse.x >= p.x && mouse.x <= p.x + Board.SQUARE_SIZE &&
                    mouse.y >= p.y && mouse.y <= p.y + Board.SQUARE_SIZE) {
                replacePawnAndFinish(p); break;
            }
        }
        mouse.released = false;
    }

    private void finalizeTurn() {
        isClickedToMove = false; activeP = null; validMoves.clear();
        castlingP = null; // Quan trọng: Giải phóng Xe để lượt sau không nhảy vị trí
        isCastling = false;
        currentColor = (currentColor == WHITE) ? BLACK : WHITE;
        for (Piece p : pieces) if (p.color == currentColor) p.twoStepped = false;
        isKingInCheck(); resetTime(); isTimeRunning = true;
    }

    // --- LƯU / TẢI (Fix lỗi image_23f735) ---
    public void saveGame(int slot, BufferedImage img) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("savegame_" + slot + ".dat"))) {
            oos.writeObject(new SaveData(new ArrayList<>(this.pieces), this.currentColor, this.timeLeft));
            if (img != null) ImageIO.write(img, "png", new File("thumbnail_" + slot + ".png"));
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void loadGame(int slot) {
        File f = new File("savegame_" + slot + ".dat"); if (!f.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            SaveData d = (SaveData) ois.readObject();
            pieces.clear(); pieces.addAll(d.pieces);
            currentColor = d.currentColor; timeLeft = d.timeLeft;
            for (Piece p : pieces) { p.image = reloadPieceImage(p); p.updatePosition(); }
            copyPieces(pieces, simPieces);
            gamePanel = new GamePanel(this); showPanel(gamePanel);
            audioManager.playBGM("res/audio/bgm/game_theme.wav");
            GameState.setState(State.PLAYING); isTimeRunning = true;
            if (gameThread == null || !gameThread.isAlive()) { gameThread = new Thread(this); gameThread.start(); }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public BufferedImage getSlotThumbnail(int slot) {
        try { File f = new File("thumbnail_" + slot + ".png"); return f.exists() ? ImageIO.read(f) : null; }
        catch (IOException e) { return null; }
    }

    public String getSlotMetadata(int slot) {
        File f = new File("savegame_" + slot + ".dat"); if (!f.exists()) return "Empty Slot";
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            SaveData d = (SaveData) ois.readObject(); return (d.saveTime != null) ? d.saveTime : "Unknown";
        } catch (Exception e) { return "Empty"; }
    }

    // --- TIỆN ÍCH (Fix lỗi image_245c6a, image_24cda4) ---
    public boolean isInsufficientMaterial() {
        for (Piece p : simPieces) if (p.type == Type.PAWN || p.type == Type.ROOK || p.type == Type.QUEEN) return false;
        return simPieces.size() <= 3;
    }

    private void updateCursorState() {
        int col = mouse.x / Board.SQUARE_SIZE, row = mouse.y / Board.SQUARE_SIZE; boolean h = false;
        if (activeP == null) {
            for (Piece p : simPieces) if (p.color == currentColor && p.col == col && p.row == row) h = true;
        } else {
            for (int[] mv : validMoves) if (mv[0] == col && mv[1] == row) h = true;
        }
        window.setCursor(Cursor.getPredefinedCursor(h ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private BufferedImage reloadPieceImage(Piece p) {
        String pre = (p.color == WHITE) ? "w" : "b";
        String name = p.type.toString().substring(0, 1).toUpperCase() + p.type.toString().substring(1).toLowerCase();
        return p.getImage("/piece/" + pre + name);
    }

    public boolean isKingInCheck() {
        Piece king = getKing(false); if (king == null) return false;
        for (Piece p : simPieces) if (p.color != king.color && p.canMove(king.col, king.row)) { checkingP = p; return true; }
        checkingP = null; return false;
    }

    public Piece getKing(boolean opponent) {
        int color = opponent ? ((currentColor == WHITE) ? BLACK : WHITE) : currentColor;
        for (Piece p : simPieces) if (p.type == Type.KING && p.color == color) return p;
        return null;
    }

    private void triggerEndGame(boolean draw, Integer win) {
        this.gameOver = true; this.isTimeRunning = false; this.isDraw = draw;
        GameState.currentState = State.GAME_OVER;
        if (draw) audioManager.playSFX("res/audio/sfx/draw.wav");
        else if (win != null) audioManager.playSFX(win == WHITE ? "res/audio/sfx/white_win.wav" : "res/audio/sfx/black_win.wav");
    }

    public void setPieces() { pieces.clear(); ChessSetupUtility.setupStandardGame(this.pieces); for (Piece p : this.pieces) { p.image = reloadPieceImage(p); p.updatePosition(); } }
    public void copyPieces(CopyOnWriteArrayList<Piece> src, CopyOnWriteArrayList<Piece> tgt) { tgt.clear(); tgt.addAll(src); }
    public void resetTime() { timeLeft = 10; lastSecond = System.currentTimeMillis(); }
    private void calculateValidMoves(Piece p) { validMoves.clear(); if (p == null) return; for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) if (p.canMove(c, r) && simulateMoveAndKingSafe(p, c, r)) { int type = (p.gettingHitP(c, r) != null || (p.type == Type.PAWN && c != p.col)) ? 1 : 0; validMoves.add(new int[] { c, r, type }); } }
    private boolean simulateMoveAndKingSafe(Piece p, int tc, int tr) { int oR = p.row, oC = p.col; Piece cap = p.gettingHitP(tc, tr); if (cap != null) simPieces.remove(cap); p.col = tc; p.row = tr; boolean safe = !opponentsCanCaptureKing(); p.col = oC; p.row = oR; if (cap != null) simPieces.add(cap); return safe; }
    private boolean opponentsCanCaptureKing() { Piece king = getKing(false); if (king == null) return false; for (Piece p : simPieces) if (p.color != king.color && p.canMove(king.col, king.row)) return true; return false; }
    private boolean isCheckMate() { if (!isKingInCheck()) return false; for (Piece p : simPieces) if (p.color == currentColor) for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) if (p.canMove(c, r) && simulateMoveAndKingSafe(p, c, r)) return false; return true; }
    private boolean isStaleMate() { if (isKingInCheck()) return false; for (Piece p : simPieces) if (p.color == currentColor) for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) if (p.canMove(c, r) && simulateMoveAndKingSafe(p, c, r)) return false; return true; }
    private void cancelOrSwitchSelection(int c, int r) { activeP = null; isClickedToMove = false; validMoves.clear(); for (Piece p : simPieces) if (p.color == currentColor && p.col == c && p.row == r) { activeP = p; calculateValidMoves(activeP); isClickedToMove = true; break; } }

    // --- GETTERS ---
    public int getCurrentColor() { return currentColor; }
    public CopyOnWriteArrayList<Piece> getSimPieces() { return simPieces; }
    public Board getBoard() { return board; }
    public Piece getActiveP() { return activeP; }
    public boolean isPromotion() { return promotion; }
    public ArrayList<Piece> getPromoPieces() { return promoPieces; }
    public boolean isGameOver() { return gameOver; }
    public boolean isDraw() { return isDraw; }
    public int getTimeLeft() { return timeLeft; }
    public boolean isClickedToMove() { return isClickedToMove; }
    public ArrayList<int[]> getValidMoves() { return validMoves; }
    public AudioManager getAudioManager() { return audioManager; }
    public Piece getCheckingP() { return checkingP; }
}