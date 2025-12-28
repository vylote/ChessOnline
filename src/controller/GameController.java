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

public class GameController implements Runnable {
    private final MainFrame window;
    private GamePanel gamePanel;
    private final MenuPanel menuPanel;
    private final PausePanel pausePanel;

    public static final int WHITE = 0;
    public static final int BLACK = 1;
    private int currentColor = WHITE;

    // Multiplayer Fields
    public boolean isMultiplayer = false;
    public boolean isServer = false;
    public int playerColor = WHITE;
    public NetworkManager netManager;

    private boolean promotion, gameOver, isDraw;
    private boolean isTimeRunning = false;
    private boolean isClickedToMove = false;
    private boolean isCastling = false;

    private long lastSecond = System.currentTimeMillis();
    private int timeLeft = 10;

    private final Board board = new Board();
    public Mouse mouse = new Mouse();
    private final CopyOnWriteArrayList<Piece> pieces = new CopyOnWriteArrayList<>();
    public static CopyOnWriteArrayList<Piece> simPieces = new CopyOnWriteArrayList<>();

    private final ArrayList<Piece> promoPieces = new ArrayList<>();
    private Piece activeP, checkingP;
    public static Piece castlingP;
    private final ArrayList<int[]> validMoves = new ArrayList<>();

    private final AudioManager audioManager;
    private Thread gameThread;

    private final String MENU_BGM = "/audio/bgm/menu_theme.wav";
    private final String GAME_BGM = "/audio/bgm/game_theme.wav";

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

    // --- MULTIPLAYER LOGIC ---
    public void setupMultiplayer(boolean host, int selectedColor, String ip) {
        this.isMultiplayer = true;
        this.isServer = host;
        this.playerColor = selectedColor;
        this.netManager = new NetworkManager(this);
        if (host) netManager.hostGame(5555);
        else netManager.joinGame(ip, 5555);
    }

    public void onConfigReceived(GameConfigPacket packet) {
        if (!isServer) {
            this.playerColor = (packet.hostColor == WHITE) ? BLACK : WHITE;
            // Chạy trên luồng UI để tránh lỗi giao diện
            javax.swing.SwingUtilities.invokeLater(() -> {
                startNewGame();
            });
        }
    }

    public void receiveNetworkMove(MovePacket packet) {
        for (Piece p : simPieces) {
            if (p.col == packet.oldCol && p.row == packet.oldRow) {
                activeP = p;
                simulateClickToMove(packet.newCol, packet.newRow);
                activeP.finishMove();
                copyPieces(simPieces, pieces);
                finalizeTurn();
                break;
            }
        }
    }

    // --- SAVE / LOAD LOGIC ---
    private String getBaseSavePath() {
        String workingDir = System.getProperty("user.dir");
        if (workingDir.toLowerCase().contains("program files")) {
            return System.getenv("LOCALAPPDATA") + File.separator + "VyChessGame";
        }
        return workingDir + File.separator + "saves";
    }

    private File getSaveFileHandle(String fileName) {
        File directory = new File(getBaseSavePath());
        if (!directory.exists()) directory.mkdirs();
        return new File(directory, fileName);
    }

    public void saveGame(int s, BufferedImage img) {
        if (isMultiplayer && !isServer) return;
        File saveFile = getSaveFileHandle("savegame_" + s + ".dat");
        File thumbFile = getSaveFileHandle("thumbnail_" + s + ".png");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(saveFile))) {
            oos.writeObject(new SaveData(new ArrayList<>(this.pieces), this.currentColor, this.timeLeft));
            if (img != null) ImageIO.write(img, "png", thumbFile);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void loadGame(int s) {
        File f = getSaveFileHandle("savegame_" + s + ".dat");
        if (!f.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            SaveData d = (SaveData) ois.readObject();
            pieces.clear();
            pieces.addAll(d.pieces);
            currentColor = d.currentColor;
            timeLeft = d.timeLeft;
            for (Piece p : pieces) {
                p.image = reloadPieceImage(p);
                p.updatePosition();
            }
            copyPieces(pieces, simPieces);
            gamePanel = new GamePanel(this);
            showPanel(gamePanel);
            audioManager.playBGM(GAME_BGM);
            GameState.setState(State.PLAYING);
            isTimeRunning = true;
            if (gameThread == null || !gameThread.isAlive()) {
                gameThread = new Thread(this);
                gameThread.start();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public BufferedImage getSlotThumbnail(int s) {
        File f = getSaveFileHandle("thumbnail_" + s + ".png");
        try { return f.exists() ? ImageIO.read(f) : null; } catch (Exception e) { return null; }
    }

    public String getSlotMetadata(int s) {
        File f = getSaveFileHandle("savegame_" + s + ".dat");
        if (!f.exists()) return "Empty";
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            SaveData d = (SaveData) ois.readObject();
            return d.saveTime;
        } catch (Exception e) { return "Empty"; }
    }

    // --- GAME ENGINE ---
    private void showPanel(JPanel panel) { window.showPanel(panel); }

    public void startNewGame() {
        setPieces();
        copyPieces(pieces, simPieces);
        currentColor = WHITE;
        isDraw = false;
        gameOver = false;
        isClickedToMove = false;
        promotion = false;
        resetTime();
        gamePanel = new GamePanel(this);
        showPanel(gamePanel);
        audioManager.playBGM(GAME_BGM);
        isTimeRunning = true;
        GameState.currentState = State.PLAYING;
        if (gameThread == null || !gameThread.isAlive()) {
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    @Override
    public void run() {
        double interval = 1000000000.0 / 60;
        double delta = 0;
        long lastTime = System.nanoTime();
        while (gameThread != null) {
            long now = System.nanoTime();
            delta += (now - lastTime) / interval;
            lastTime = now;
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
                if (promotion && !promoPieces.isEmpty()) replacePawnAndFinish(promoPieces.get(0));
                else if (isKingInCheck()) triggerEndGame(false, (currentColor == WHITE ? BLACK : WHITE));
                else finalizeTurn();
                return;
            }
        }
        if (promotion) promoting();
        else handleMouseInput();
    }

    public int getDisplayCol(int col) {
        return (isMultiplayer && playerColor == BLACK) ? 7 - col : col;
    }

    public int getDisplayRow(int row) {
        return (isMultiplayer && playerColor == BLACK) ? 7 - row : row;
    }

    private void handleMouseInput() {
        if (isMultiplayer && currentColor != playerColor) return;
        updateCursorState();
        if (!mouse.released) return;
        int col = mouse.x / Board.SQUARE_SIZE, row = mouse.y / Board.SQUARE_SIZE;

        // Đảo ngược tọa độ click nếu người chơi đang xem bàn cờ bị lật
        if (isMultiplayer && playerColor == BLACK) {
            col = 7 - col;
            row = 7 - row;
        }

        if (activeP == null) {
            for (Piece piece : simPieces)
                if (piece.color == currentColor && piece.col == col && piece.row == row) {
                    activeP = piece;
                    calculateValidMoves(activeP);
                    isClickedToMove = true;
                    break;
                }
        } else {
            boolean isMove = false;
            int oldCol = activeP.col, oldRow = activeP.row;
            for (int[] mv : validMoves) if (mv[0] == col && mv[1] == row) { isMove = true; break; }
            if (isMove) {
                Piece captured = activeP.gettingHitP(col, row);
                simulateClickToMove(col, row);
                activeP.finishMove();
                copyPieces(simPieces, pieces);
                if (isMultiplayer && netManager != null) netManager.sendMove(new MovePacket(oldCol, oldRow, col, row));
                playMoveSound(captured != null);
                if (isKingInCheck() && isCheckMate()) triggerEndGame(false, (currentColor == WHITE ? BLACK : WHITE));
                else if (isStaleMate()) triggerEndGame(true, null);
                else { if (canPromote()) promotion = true; else finalizeTurn(); }
            } else cancelOrSwitchSelection(col, row);
        }
        mouse.released = false;
    }

    private void playMoveSound(boolean capture) {
        if (capture) audioManager.playSFX("/audio/sfx/capture.wav");
        else audioManager.playSFX("/audio/sfx/move.wav");
        Timer t = new Timer(200, e -> { if (isKingInCheck() && !gameOver) audioManager.playSFX("/audio/sfx/check.wav"); });
        t.setRepeats(false);
        t.start();
    }

    public void finalizeTurn() {
        isClickedToMove = false;
        activeP = null;
        validMoves.clear();
        castlingP = null;
        isCastling = false;
        currentColor = (currentColor == WHITE) ? BLACK : WHITE;
        for (Piece p : pieces) if (p.color == currentColor) p.twoStepped = false;
        isKingInCheck();
        resetTime();
        isTimeRunning = true;
    }

    public void simulateClickToMove(int tc, int tr) {
        copyPieces(pieces, simPieces);
        activeP.canMove(tc, tr);
        if (activeP.hittingP != null) simPieces.remove(activeP.hittingP);
        activeP.col = tc;
        activeP.row = tr;
    }

    // Getters
    public MenuPanel getMenuPanel() { return menuPanel; }
    public int getCurrentColor() { return currentColor; }
    public boolean isGameOver() { return gameOver; }
    public boolean isDraw() { return isDraw; }
    public boolean isPromotion() { return promotion; }
    public boolean isClickedToMove() { return isClickedToMove; }
    public int getTimeLeft() { return timeLeft; }
    public Piece getActiveP() { return activeP; }
    public Piece getCheckingP() { return checkingP; }
    public Board getBoard() { return board; }
    public AudioManager getAudioManager() { return audioManager; }
    public CopyOnWriteArrayList<Piece> getSimPieces() { return simPieces; }
    public ArrayList<Piece> getPromoPieces() { return promoPieces; }
    public ArrayList<int[]> getValidMoves() { return validMoves; }

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
        audioManager.playBGM(MENU_BGM);
        showPanel(menuPanel);
    }

    public void handleMouseRelease(int x, int y) { mouse.released = true; mouse.x = x; mouse.y = y; }
    public void resetTime() { timeLeft = 10; lastSecond = System.currentTimeMillis(); }

    private void setPieces() {
        pieces.clear();
        ChessSetupUtility.setupStandardGame(this.pieces);
        for (Piece p : this.pieces) { p.image = reloadPieceImage(p); p.updatePosition(); }
    }

    public void copyPieces(CopyOnWriteArrayList<Piece> s, CopyOnWriteArrayList<Piece> t) { t.clear(); t.addAll(s); }

    private BufferedImage reloadPieceImage(Piece p) {
        String pr = (p.color == WHITE) ? "w" : "b";
        String n = p.type.toString().substring(0, 1).toUpperCase() + p.type.toString().substring(1).toLowerCase();
        return p.getImage("/piece/" + pr + n);
    }

    public Piece getKing(boolean opp) {
        int c = opp ? (currentColor == WHITE ? BLACK : WHITE) : currentColor;
        for (Piece p : simPieces) if (p.type == Type.KING && p.color == c) return p;
        return null;
    }

    private void triggerEndGame(boolean d, Integer w) {
        this.gameOver = true;
        this.isTimeRunning = false;
        this.isDraw = d;
        GameState.currentState = State.GAME_OVER;
        if (d) audioManager.playSFX("/audio/sfx/draw.wav");
        else if (w != null) audioManager.playSFX(w == WHITE ? "/audio/sfx/white_win.wav" : "/audio/sfx/black_win.wav");
    }

    private void calculateValidMoves(Piece p) {
        validMoves.clear();
        if (p == null) return;
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (p.canMove(c, r) && simulateMoveAndKingSafe(p, c, r)) {
                    int t = (p.gettingHitP(c, r) != null || (p.type == Type.PAWN && c != p.col)) ? 1 : 0;
                    validMoves.add(new int[]{c, r, t});
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

    private boolean opponentsCanCaptureKing() {
        Piece king = getKing(false);
        if (king == null) return false;
        for (Piece p : simPieces) if (p.color != king.color && p.canMove(king.col, king.row)) return true;
        return false;
    }

    public boolean isKingInCheck() {
        Piece king = getKing(false);
        if (king == null) return false;
        for (Piece p : simPieces)
            if (p.color != king.color && p.canMove(king.col, king.row)) { checkingP = p; return true; }
        checkingP = null;
        return false;
    }

    private boolean isCheckMate() {
        if (!isKingInCheck()) return false;
        for (Piece p : simPieces) if (p.color == currentColor)
            for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++)
                if (p.canMove(c, r) && simulateMoveAndKingSafe(p, c, r)) return false;
        return true;
    }

    private boolean isStaleMate() {
        if (isKingInCheck()) return false;
        for (Piece p : simPieces) if (p.color == currentColor)
            for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++)
                if (p.canMove(c, r) && simulateMoveAndKingSafe(p, c, r)) return false;
        return true;
    }

    private void cancelOrSwitchSelection(int c, int r) {
        activeP = null; isClickedToMove = false; validMoves.clear();
        for (Piece p : simPieces)
            if (p.color == currentColor && p.col == c && p.row == r) {
                activeP = p; calculateValidMoves(activeP); isClickedToMove = true;
                break;
            }
    }

    private void updateCursorState() {
        int col = mouse.x / Board.SQUARE_SIZE, row = mouse.y / Board.SQUARE_SIZE;
        boolean h = false;
        if (activeP == null) {
            for (Piece p : simPieces) if (p.color == currentColor && p.col == col && p.row == row) { h = true; break; }
        } else {
            for (int[] mv : validMoves) if (mv[0] == col && mv[1] == row) { h = true; break; }
        }
        window.setCursor(Cursor.getPredefinedCursor(h ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    public boolean isInsufficientMaterial() {
        for (Piece p : simPieces) if (p.type == Type.PAWN || p.type == Type.ROOK || p.type == Type.QUEEN) return false;
        return simPieces.size() <= 3;
    }

    private boolean canPromote() {
        if (activeP == null || activeP.type != Type.PAWN) return false;
        return (activeP.color == WHITE && activeP.row == 0) || (activeP.color == BLACK && activeP.row == 7);
    }

    private void replacePawnAndFinish(Piece p) { /* Code thăng cấp */ }
    private void promoting() { /* Code hiển thị UI thăng cấp */ }
}