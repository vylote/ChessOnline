package controller;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;

import model.*;
import utility.AudioManager;
import view.GamePanel;
import view.MainFrame;
import view.MenuPanel;
import view.PausePanel;

/**
 * Controller trung tâm quản lý toàn bộ logic game, âm thanh và điều phối giao diện.
 * Sử dụng kiến trúc Single Frame để đảm bảo chỉ có 1 cửa sổ duy nhất suốt quá trình chơi.
 */
public class GameController implements Runnable {
    // --- GIAO DIỆN DUY NHẤT (SINGLE FRAME) ---
    private final MainFrame window;
    private GamePanel gamePanel;
    private final MenuPanel menuPanel;
    private final PausePanel pausePanel;

    // --- TRẠNG THÁI GAME ---
    public static final int WHITE = 0;
    public static final int BLACK = 1;
    private int currentColor = WHITE;

    private boolean promotion, gameOver, isDraw;
    private boolean isTimeRunning = false;
    private boolean isClickedToMove = false;
    private boolean isCastling = false;

    private long lastSecond = System.currentTimeMillis();
    private int timeLeft = 10;

    // --- THÀNH PHẦN LOGIC ---
    private final Board board = new Board();
    public Mouse mouse = new Mouse();
    private final ArrayList<Piece> pieces = new ArrayList<>();
    public static ArrayList<Piece> simPieces = new ArrayList<>();
    private final ArrayList<Piece> promoPieces = new ArrayList<>();
    private Piece activeP, checkingP;
    public static Piece castlingP;
    private final ArrayList<int[]> validMoves = new ArrayList<>();

    // --- HỆ THỐNG ÂM THANH ---
    private final AudioManager audioManager;
    private final String MENU_BGM = "res/audio/bgm/menu_theme.wav";
    private final String GAME_BGM = "res/audio/bgm/game_theme.wav";

    private final String SFX_MOVE    = "res/audio/sfx/move.wav";
    private final String SFX_CAPTURE = "res/audio/sfx/capture.wav";
    private final String SFX_CHECK   = "res/audio/sfx/check.wav";
    private final String SFX_CASTLE  = "res/audio/sfx/castle.wav";
    private final String SFX_PROMOTE = "res/audio/sfx/promote.wav";

    private final String SFX_WHITE_WIN = "res/audio/sfx/white_win.wav";
    private final String SFX_BLACK_WIN = "res/audio/sfx/black_win.wav";
    private final String SFX_DRAW      = "res/audio/sfx/draw.wav";

    private Thread gameThread;
    private BufferedImage gameSnapshot;

    public GameController() {
        this.audioManager = new AudioManager();

        // 1. Khởi tạo JFrame duy nhất (Đặt nền đen để tránh màn hình trắng khi khởi động)
        this.window = new MainFrame();
        this.window.getContentPane().setBackground(Color.BLACK);

        // 2. Khởi tạo các Panel
        this.menuPanel = new MenuPanel(this, window);
        this.pausePanel = new PausePanel(this, window);

        // 3. Hiển thị Menu đầu tiên
        showPanel(menuPanel);
        audioManager.playBGM(MENU_BGM);
        GameState.setState(State.MENU);

        setPieces();
        copyPieces(pieces, simPieces);
    }

    private void showPanel(JPanel panel) {
        window.showPanel(panel);
    }

    // --- GAME CONTROL METHODS ---

    public void startNewGame() {
        setPieces();
        copyPieces(pieces, simPieces);

        currentColor = WHITE;
        isDraw = false; gameOver = false; isClickedToMove = false; promotion = false;
        resetTime();

        // Tạo ván mới
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

            // Chụp ảnh bàn cờ để làm nền mờ và thumbnail
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
        audioManager.playBGM(MENU_BGM);
        showPanel(menuPanel);
    }

    public void handleMouseRelease(int x, int y) {
        mouse.released = true;
        mouse.x = x;
        mouse.y = y;
    }

    // --- CORE LOOP ---

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
        isKingInCheck();

        if (isInsufficientMaterial()) {
            triggerEndGame(true, null);
            return;
        }

        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - lastSecond >= 1000 && isTimeRunning) {
            lastSecond = currentTimeMillis;
            timeLeft--;

            if (timeLeft <= 0) {
                if (promotion) autoPromoteOnTimeout();
                else if (isKingInCheck()) triggerEndGame(false, currentColor == WHITE ? BLACK : WHITE);
                else finalizeTurn();
            }
        }

        if (promotion) promoting(); else handleMouseInput();
    }

    private void handleMouseInput() {
        // Cập nhật Cursor Pointer cho các quân cờ có thể click
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

                // Âm thanh di chuyển/bắt quân
                if (captured != null) audioManager.playSFX(SFX_CAPTURE);
                else if (isCastling) { audioManager.playSFX(SFX_CASTLE); isCastling = false; }
                else audioManager.playSFX(SFX_MOVE);

                if (castlingP != null) castlingP.updatePosition();

                // Âm thanh trạng thái (Check/Win) với độ trễ 200ms
                Timer sfxTimer = new Timer(200, e -> {
                    if (isKingInCheck() && isCheckMate()) triggerEndGame(false, currentColor == WHITE ? BLACK : WHITE);
                    else if (isStaleMate()) triggerEndGame(true, null);
                    else if (isKingInCheck()) audioManager.playSFX(SFX_CHECK);
                    ((Timer)e.getSource()).stop();
                });
                sfxTimer.setRepeats(false);
                sfxTimer.start();

                if (isKingInCheck() && isCheckMate()) setInternalGameOver(false, currentColor == WHITE ? BLACK : WHITE);
                else if (isStaleMate()) setInternalGameOver(true, null);
                else {
                    if (canPromote()) promotion = true; else finalizeTurn();
                }
            } else {
                cancelOrSwitchSelection(col, row);
            }
        }
        mouse.released = false;
    }

    private void updateCursorState() {
        int col = mouse.x / Board.SQUARE_SIZE;
        int row = mouse.y / Board.SQUARE_SIZE;
        boolean hoveringInteractable = false;

        if (activeP == null) {
            for (Piece piece : simPieces) {
                if (piece.color == currentColor && piece.col == col && piece.row == row) {
                    hoveringInteractable = true; break;
                }
            }
        } else {
            for (int[] mv : validMoves) {
                if (mv[0] == col && mv[1] == row) { hoveringInteractable = true; break; }
            }
        }
        window.setCursor(Cursor.getPredefinedCursor(hoveringInteractable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    // --- GAME LOGIC METHODS ---

    private void autoPromoteOnTimeout() {
        Piece p = new Queen(currentColor, activeP.row, activeP.col);
        replacePawnAndFinish(p);
    }

    private void replacePawnAndFinish(Piece p) {
        Piece newP = null;
        int r = activeP.row, c = activeP.col;
        switch (p.type) {
            case QUEEN:  newP = new Queen(currentColor, r, c); break;
            case ROOK:   newP = new Rook(currentColor, r, c); break;
            case BISHOP: newP = new Bishop(currentColor, r, c); break;
            case KNIGHT: newP = new Knight(currentColor, r, c); break;
        }
        if (newP != null) {
            newP.image = reloadPieceImage(newP);
            newP.updatePosition();
            simPieces.remove(activeP);
            simPieces.add(newP);
            copyPieces(simPieces, pieces);
        }

        // Phát âm thanh phong cấp sau một chút
        Timer promoDelay = new Timer(150, e -> {
            audioManager.playSFX(SFX_PROMOTE);
            ((Timer)e.getSource()).stop();
        });
        promoDelay.setRepeats(false);
        promoDelay.start();

        promotion = false;
        finalizeTurn();
    }

    private void promoting() {
        int selCol = mouse.x / Board.SQUARE_SIZE;
        int selRow = mouse.y / Board.SQUARE_SIZE;

        // Hover effect cho danh sách phong cấp
        boolean hoveringPromo = false;
        for (Piece p : promoPieces) {
            if (p.col == selCol && p.row == selRow) { hoveringPromo = true; break; }
        }
        window.setCursor(Cursor.getPredefinedCursor(hoveringPromo ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));

        if (!mouse.released) return;

        for (Piece p : promoPieces) {
            if (p.col == selCol && p.row == selRow) {
                replacePawnAndFinish(p);
                break;
            }
        }
        mouse.released = false;
    }

    private void finalizeTurn() {
        isClickedToMove = false;
        activeP = null;
        validMoves.clear();
        changePlayer();
        resetTime();
        isTimeRunning = true;
    }

    private void changePlayer() {
        currentColor = (currentColor == WHITE) ? BLACK : WHITE;
        for (Piece piece : pieces) if (piece.color == currentColor) piece.twoStepped = false;
    }

    public void setPieces() {
        pieces.clear();
        ChessSetupUtility.setupStandardGame(this.pieces);
        for (Piece p : this.pieces) {
            p.image = reloadPieceImage(p);
            p.updatePosition();
        }
        isKingInCheck();
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
        int oldRow = piece.row, oldCol = piece.col;
        Piece captured = piece.gettingHitP(targetCol, targetRow);
        if (captured != null) simPieces.remove(captured);
        piece.col = targetCol; piece.row = targetRow;
        boolean kingSafe = !opponentsCanCaptureKing();
        piece.col = oldCol; piece.row = oldRow;
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
        for (Piece piece : simPieces) {
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
        for (Piece piece : simPieces) {
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
        for (Piece piece : simPieces) {
            if (piece.color != king.color && piece.canMove(king.col, king.row)) return true;
        }
        return false;
    }

    public boolean isKingInCheck() {
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

    public Piece getKing(boolean opponent) {
        int colorToFind = opponent ? (currentColor == WHITE ? BLACK : WHITE) : currentColor;
        for (Piece piece : simPieces) {
            if (piece.type == Type.KING && piece.color == colorToFind) return piece;
        }
        return null;
    }

    private boolean canPromote() {
        if (activeP == null || activeP.type != Type.PAWN) return false;
        if ((activeP.color == WHITE && activeP.row == 0) || (activeP.color == BLACK && activeP.row == 7)) {
            promoPieces.clear();
            int color = activeP.color;
            promoPieces.add(new Queen(color, 2, 9)); // Vị trí giả cho UI
            promoPieces.add(new Rook(color, 3, 9));
            promoPieces.add(new Knight(color, 4, 9));
            promoPieces.add(new Bishop(color, 5, 9));
            for (Piece p : promoPieces) p.image = reloadPieceImage(p);
            return true;
        }
        return false;
    }

    private boolean isInsufficientMaterial() {
        for (Piece p : simPieces) if (p.type == Type.PAWN || p.type == Type.ROOK || p.type == Type.QUEEN) return false;
        return simPieces.size() <= 3;
    }

    // --- END GAME HANDLER ---

    private void setInternalGameOver(boolean draw, Integer winnerColor) {
        this.gameOver = true;
        this.isTimeRunning = false;
        this.isDraw = draw;
        GameState.currentState = State.GAME_OVER;
    }

    private void triggerEndGame(boolean draw, Integer winnerColor) {
        setInternalGameOver(draw, winnerColor);
        if (draw) audioManager.playSFX(SFX_DRAW);
        else if (winnerColor != null) {
            audioManager.playSFX(winnerColor == WHITE ? SFX_WHITE_WIN : SFX_BLACK_WIN);
        }
    }

    // --- SAVE / LOAD ---

    public void saveGame(int slot, BufferedImage snapshot) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("savegame_" + slot + ".dat"))) {
            SaveData data = new SaveData(this.pieces, this.currentColor, this.timeLeft);
            oos.writeObject(data);
            if (snapshot != null) {
                ImageIO.write(snapshot, "png", new File("thumbnail_" + slot + ".png"));
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public BufferedImage getSlotThumbnail(int slot) {
        try {
            File f = new File("thumbnail_" + slot + ".png");
            return f.exists() ? ImageIO.read(f) : null;
        } catch (IOException e) { return null; }
    }

    public void loadGame(int slot) {
        File file = new File("savegame_" + slot + ".dat");
        if (!file.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            SaveData data = (SaveData) ois.readObject();
            this.pieces.clear();
            this.pieces.addAll(data.pieces);
            this.currentColor = data.currentColor;
            this.timeLeft = data.timeLeft;
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
        File file = new File("savegame_" + slot + ".dat");
        if (!file.exists()) return "Empty Slot";
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            SaveData data = (SaveData) ois.readObject();
            return data.saveTime != null ? data.saveTime : "Unknown Date";
        } catch (Exception e) { return "Corrupted Data"; }
    }

    private void cancelOrSwitchSelection(int col, int row) {
        activeP = null; isClickedToMove = false; validMoves.clear();
        for (Piece piece : simPieces) {
            if (piece.color == currentColor && piece.col == col && piece.row == row) {
                activeP = piece; calculateValidMoves(activeP); isClickedToMove = true; break;
            }
        }
    }

    private BufferedImage reloadPieceImage(Piece p) {
        String prefix = (p.color == WHITE) ? "w" : "b";
        String typeName = p.type.toString().substring(0, 1).toUpperCase() + p.type.toString().substring(1).toLowerCase();
        return p.getImage("/piece/" + prefix + typeName);
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
    public AudioManager getAudioManager() { return audioManager; }
}