package controller;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList; // Import quan trọng cho Cách 1
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;

import model.*;
import utility.AudioManager;
import view.GamePanel;
import view.MainFrame;
import view.MenuPanel;
import view.PausePanel;

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

    // --- THÀNH PHẦN LOGIC (ÁP DỤNG CÁCH 1) ---
    private final Board board = new Board();
    public Mouse mouse = new Mouse();

    // Sử dụng CopyOnWriteArrayList để tránh ConcurrentModificationException
    private final CopyOnWriteArrayList<Piece> pieces = new CopyOnWriteArrayList<>();
    public static CopyOnWriteArrayList<Piece> simPieces = new CopyOnWriteArrayList<>();

    private final ArrayList<Piece> promoPieces = new ArrayList<>();
    private Piece activeP, checkingP;
    public static Piece castlingP;
    private final ArrayList<int[]> validMoves = new ArrayList<>();

    // --- ÂM THANH ---
    private final AudioManager audioManager;
    private final String MENU_BGM = "res/audio/bgm/menu_theme.wav";
    private final String GAME_BGM = "res/audio/bgm/game_theme.wav";

    private Thread gameThread;
    private BufferedImage gameSnapshot;

    public GameController() {
        this.audioManager = new AudioManager();
        this.window = new MainFrame();
        this.window.getContentPane().setBackground(Color.BLACK);
        this.menuPanel = new MenuPanel(this, window);
        this.pausePanel = new PausePanel(this, window);

        showPanel(menuPanel);
        audioManager.playBGM(MENU_BGM);
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
        resetTime();

        gamePanel = new GamePanel(this);
        showPanel(gamePanel);
        audioManager.playBGM(GAME_BGM);

        // Kiểm tra trạng thái tức thì
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

    public void pauseGame() {
        if (GameState.currentState == State.PLAYING) {
            isTimeRunning = false;
            GameState.setState(State.PAUSED);
            this.gameSnapshot = gamePanel.getGameSnapshot();
            pausePanel.setBackgroundSnapshot(this.gameSnapshot);
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
        audioManager.playBGM(MENU_BGM);
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
                if (gamePanel != null && GameState.currentState == State.PLAYING) {
                    gamePanel.repaint();
                }
                delta--;
            }
        }
    }

    private void update() {
        if (GameState.currentState != State.PLAYING || gameOver) return;

        // Cập nhật King bị chiếu liên tục cho View tô đỏ
        isKingInCheck();

        if (isInsufficientMaterial()) {
            triggerEndGame(true, null);
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastSecond >= 1000 && isTimeRunning) {
            lastSecond = now;
            timeLeft--;

            if (timeLeft <= 0) {
                isTimeRunning = false;
                // NẾU HẾT GIỜ -> THUA NGAY (BẤT KỂ ĐANG BỊ CHIẾU HAY KHÔNG)
                triggerEndGame(false, currentColor == WHITE ? BLACK : WHITE);
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
            for (int[] mv : validMoves) {
                if (mv[0] == col && mv[1] == row) { isMove = true; break; }
            }

            if (isMove) {
                Piece captured = activeP.gettingHitP(col, row);
                simulateClickToMove(col, row);
                activeP.finishMove();
                copyPieces(simPieces, pieces);

                // PHÁT TIẾNG MOVE/CAPTURE TỨC THỜI
                if (captured != null) audioManager.playSFX("res/audio/sfx/capture.wav");
                else if (isCastling) { audioManager.playSFX("res/audio/sfx/castle.wav"); isCastling = false; }
                else audioManager.playSFX("res/audio/sfx/move.wav");

                if (castlingP != null) castlingP.updatePosition();

                // ĐỘ TRỄ 200ms CHO TIẾNG CHECK/WIN
                Timer sfxTimer = new Timer(200, e -> {
                    if (isKingInCheck() && !gameOver) {
                        audioManager.playSFX("res/audio/sfx/check.wav");
                    }
                    ((Timer)e.getSource()).stop();
                });
                sfxTimer.setRepeats(false);
                sfxTimer.start();

                // CẬP NHẬT TRẠNG THÁI GAME
                if (isKingInCheck() && isCheckMate()) {
                    triggerEndGame(false, currentColor == WHITE ? BLACK : WHITE);
                } else if (isStaleMate()) {
                    triggerEndGame(true, null);
                } else {
                    if (canPromote()) promotion = true; else finalizeTurn();
                }
            } else {
                cancelOrSwitchSelection(col, row);
            }
        }
        mouse.released = false;
    }

    private void finalizeTurn() {
        isClickedToMove = false;
        activeP = null;
        validMoves.clear();
        changePlayer();
        isKingInCheck(); // Đảm bảo King đối phương đỏ ngay khi vừa đổi lượt
        resetTime();
        isTimeRunning = true;
    }

    private void triggerEndGame(boolean draw, Integer winnerColor) {
        this.gameOver = true;
        this.isTimeRunning = false;
        this.isDraw = draw;
        GameState.currentState = State.GAME_OVER;

        if (draw) audioManager.playSFX("res/audio/sfx/draw.wav");
        else if (winnerColor != null) {
            audioManager.playSFX(winnerColor == WHITE ? "res/audio/sfx/white_win.wav" : "res/audio/sfx/black_win.wav");
        }
    }

    private void changePlayer() {
        currentColor = (currentColor == WHITE) ? BLACK : WHITE;
        for (Piece p : pieces) if (p.color == currentColor) p.twoStepped = false;
    }

    public boolean isKingInCheck() {
        Piece king = getKing(false);
        if (king == null) return false;
        for (Piece p : simPieces) {
            if (p.color != king.color && p.canMove(king.col, king.row)) {
                checkingP = p;
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

    public Piece getCheckingP() { return checkingP; }

    public void setPieces() {
        pieces.clear();
        ChessSetupUtility.setupStandardGame(this.pieces);
        for (Piece p : this.pieces) {
            p.image = reloadPieceImage(p);
            p.updatePosition();
        }
    }

    public void copyPieces(CopyOnWriteArrayList<Piece> src, CopyOnWriteArrayList<Piece> tgt) {
        tgt.clear();
        tgt.addAll(src);
    }

    public void resetTime() {
        timeLeft = 10;
        lastSecond = System.currentTimeMillis();
    }

    private void calculateValidMoves(Piece p) {
        validMoves.clear();
        if (p == null) return;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (p.canMove(c, r) && simulateMoveAndKingSafe(p, c, r)) {
                    int type = (p.gettingHitP(c, r) != null || (p.type == Type.PAWN && c != p.col)) ? 1 : 0;
                    validMoves.add(new int[] { c, r, type });
                }
            }
        }
    }

    private boolean simulateMoveAndKingSafe(Piece p, int tc, int tr) {
        int oR = p.row, oC = p.col;
        Piece cap = p.gettingHitP(tc, tr);
        if (cap != null) simPieces.remove(cap);
        p.col = tc; p.row = tr;
        boolean safe = !opponentsCanCaptureKing();
        p.col = oC; p.row = oR;
        if (cap != null) simPieces.add(cap);
        return safe;
    }

    public void simulateClickToMove(int tc, int tr) {
        copyPieces(pieces, simPieces);
        if (castlingP != null) {
            castlingP.col = castlingP.preCol;
            castlingP.x = castlingP.getX(castlingP.col);
            castlingP = null;
        }
        activeP.canMove(tc, tr);
        if (activeP.hittingP != null) simPieces.remove(activeP.hittingP);
        activeP.col = tc;
        activeP.row = tr;
        if (castlingP != null) {
            if (castlingP.col == 0) castlingP.col += 3;
            else if (castlingP.col == 7) castlingP.col -= 2;
            castlingP.x = castlingP.getX(castlingP.col);
            isCastling = true;
        }
    }

    private boolean opponentsCanCaptureKing() {
        Piece king = getKing(false);
        if (king == null) return false;
        for (Piece p : simPieces) if (p.color != king.color && p.canMove(king.col, king.row)) return true;
        return false;
    }

    private boolean isCheckMate() {
        if (!isKingInCheck()) return false;
        for (Piece p : simPieces) {
            if (p.color != currentColor) continue;
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    if (p.canMove(c, r) && simulateMoveAndKingSafe(p, c, r)) return false;
                }
            }
        }
        return true;
    }

    private boolean isStaleMate() {
        if (isKingInCheck()) return false;
        for (Piece p : simPieces) {
            if (p.color != currentColor) continue;
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    if (p.canMove(c, r) && simulateMoveAndKingSafe(p, c, r)) return false;
                }
            }
        }
        return true;
    }

    private boolean canPromote() {
        if (activeP == null || activeP.type != Type.PAWN) return false;
        return (activeP.color == WHITE && activeP.row == 0) || (activeP.color == BLACK && activeP.row == 7);
    }

    private void promoting() {
        int selCol = mouse.x / Board.SQUARE_SIZE, selRow = mouse.y / Board.SQUARE_SIZE;
        if (!mouse.released) return;
        for (Piece p : promoPieces) {
            if (p.col == selCol && p.row == selRow) { replacePawnAndFinish(p); break; }
        }
        mouse.released = false;
    }

    private void replacePawnAndFinish(Piece p) {
        Piece newP = null;
        if (p.type == Type.QUEEN) newP = new Queen(currentColor, activeP.row, activeP.col);
        else if (p.type == Type.ROOK) newP = new Rook(currentColor, activeP.row, activeP.col);
        else if (p.type == Type.BISHOP) newP = new Bishop(currentColor, activeP.row, activeP.col);
        else if (p.type == Type.KNIGHT) newP = new Knight(currentColor, activeP.row, activeP.col);
        if (newP != null) {
            newP.image = reloadPieceImage(newP);
            newP.updatePosition();
            simPieces.remove(activeP);
            simPieces.add(newP);
            copyPieces(simPieces, pieces);
        }
        audioManager.playSFX("res/audio/sfx/promote.wav");
        promotion = false;
        finalizeTurn();
    }

    private boolean isInsufficientMaterial() {
        for (Piece p : simPieces) if (p.type == Type.PAWN || p.type == Type.ROOK || p.type == Type.QUEEN) return false;
        return simPieces.size() <= 3;
    }

    public void saveGame(int slot, BufferedImage snapshot) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("savegame_" + slot + ".dat"))) {
            oos.writeObject(new SaveData(new ArrayList<>(this.pieces), this.currentColor, this.timeLeft));
            if (snapshot != null) ImageIO.write(snapshot, "png", new File("thumbnail_" + slot + ".png"));
        } catch (Exception e) { e.printStackTrace(); }
    }

    public BufferedImage getSlotThumbnail(int slot) {
        try { File f = new File("thumbnail_" + slot + ".png"); return f.exists() ? ImageIO.read(f) : null; }
        catch (Exception e) { return null; }
    }

    public void loadGame(int slot) {
        File f = new File("savegame_" + slot + ".dat"); if (!f.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            SaveData d = (SaveData) ois.readObject();
            this.pieces.clear();
            this.pieces.addAll(d.pieces);
            this.currentColor = d.currentColor;
            this.timeLeft = d.timeLeft;
            for (Piece p : this.pieces) { p.image = reloadPieceImage(p); p.updatePosition(); }
            copyPieces(this.pieces, simPieces);
            gamePanel = new GamePanel(this);
            showPanel(gamePanel);
            audioManager.playBGM(GAME_BGM);
            GameState.setState(State.PLAYING);
            isTimeRunning = true;
            if (gameThread == null || !gameThread.isAlive()) { gameThread = new Thread(this); gameThread.start(); }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public String getSlotMetadata(int slot) {
        File f = new File("savegame_" + slot + ".dat"); if (!f.exists()) return "Empty Slot";
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            SaveData d = (SaveData) ois.readObject(); return d.saveTime != null ? d.saveTime : "Unknown Date";
        } catch (Exception e) { return "Corrupted Data"; }
    }

    private BufferedImage reloadPieceImage(Piece p) {
        String pre = (p.color == WHITE) ? "w" : "b";
        String name = p.type.toString().substring(0, 1).toUpperCase() + p.type.toString().substring(1).toLowerCase();
        return p.getImage("/piece/" + pre + name);
    }

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

    private void updateCursorState() {
        int col = mouse.x / Board.SQUARE_SIZE; int row = mouse.y / Board.SQUARE_SIZE; boolean h = false;
        if (activeP == null) { for (Piece p : simPieces) if (p.color == currentColor && p.col == col && p.row == row) h = true; }
        else { for (int[] mv : validMoves) if (mv[0] == col && mv[1] == row) h = true; }
        window.setCursor(Cursor.getPredefinedCursor(h ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void cancelOrSwitchSelection(int col, int row) {
        activeP = null; isClickedToMove = false; validMoves.clear();
        for (Piece piece : simPieces) {
            if (piece.color == currentColor && piece.col == col && piece.row == row) {
                activeP = piece; calculateValidMoves(activeP); isClickedToMove = true; break;
            }
        }
    }
}