package controller;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;

import model.*;
import utility.AudioManager;
import view.*;

public class GameController implements Runnable {
    // --- 1. FIELDS: UI & HỆ THỐNG ---
    private final MainFrame window;
    private GamePanel gamePanel;
    private final MenuPanel menuPanel;
    private final PausePanel pausePanel;
    private Thread gameThread;
    private final AudioManager audioManager;
    private final String MENU_BGM = "/audio/bgm/menu_theme.wav";
    private final String GAME_BGM = "/audio/bgm/game_theme.wav";

    // --- 2. FIELDS: LOGIC CỜ ---
    public static final int WHITE = 0;
    public static final int BLACK = 1;
    private int currentColor = WHITE;
    private final Board board = new Board();
    public Mouse mouse = new Mouse();
    private final CopyOnWriteArrayList<Piece> pieces = new CopyOnWriteArrayList<>();
    public static CopyOnWriteArrayList<Piece> simPieces = new CopyOnWriteArrayList<>();
    private Piece activeP, checkingP;
    public static Piece castlingP;
    private final ArrayList<int[]> validMoves = new ArrayList<>();
    private final ArrayList<Piece> promoPieces = new ArrayList<>();

    // --- 3. FIELDS: TRẠNG THÁI GAME ---
    private boolean promotion, gameOver, isDraw;
    private boolean isTimeRunning = false;
    private boolean isClickedToMove = false;
    private float toastAlpha = 0;
    private int timeLeft = 10;
    private long lastSecond = System.currentTimeMillis();

    // --- 4. FIELDS: MULTIPLAYER ---
    public boolean isMultiplayer = false;
    public boolean isServer = false;
    public int playerColor = WHITE;
    public NetworkManager netManager;

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

    // =========================================================
    // NHÓM 1: ENGINE (VÒNG LẶP GAME)
    // =========================================================
    public void startNewGame() {
        pieces.clear(); simPieces.clear();
        setPieces(); copyPieces(pieces, simPieces);
        currentColor = WHITE; isDraw = false; gameOver = false;
        isClickedToMove = false; promotion = false; activeP = null; checkingP = null;
        validMoves.clear(); resetTime();
        gamePanel = new GamePanel(this);
        showPanel(gamePanel);
        audioManager.playBGM(GAME_BGM);
        isTimeRunning = true;
        GameState.currentState = State.PLAYING;
        if (gameThread == null || !gameThread.isAlive()) {
            gameThread = new Thread(this); gameThread.start();
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
        if (toastAlpha > 0) {
            toastAlpha -= 0.016f; if (toastAlpha < 0) toastAlpha = 0;
        }
        isKingInCheck();
        if (isInsufficientMaterial()) { triggerEndGame(true, null); return; }
        long now = System.currentTimeMillis();
        if (isTimeRunning && now - lastSecond >= 1000) {
            lastSecond = now; timeLeft--;
            if (timeLeft <= 0) { handleTimeOut(); return; }
        }
        if (promotion) promoting(); else handleMouseInput();
    }

    private void handleTimeOut() {
        if (promotion && !promoPieces.isEmpty()) replacePawnAndFinish(promoPieces.get(0));
        else if (isKingInCheck()) triggerEndGame(false, (currentColor == WHITE ? BLACK : WHITE));
        else finalizeTurn();
    }

    // =========================================================
    // NHÓM 2: ĐIỀU HƯỚNG & MENU
    // =========================================================
    public void requestRematch() {
        if (!isMultiplayer) startNewGame();
        else {
            netManager.sendMove(new MovePacket(-1, -1, -1, -1, -1));
            JOptionPane.showMessageDialog(window, "Đã gửi yêu cầu chơi lại!");
        }
    }

    public void handleRematchReceived() {
        int res = JOptionPane.showConfirmDialog(window, "Đối thủ muốn chơi lại?", "Rematch", JOptionPane.YES_NO_OPTION);
        if (res == JOptionPane.YES_OPTION) {
            if (isMultiplayer) netManager.sendMove(new MovePacket(-2, -2, -2, -2, -1));
            startNewGame();
        } else exitToMenu();
    }

    public void pauseGame() {
        if (isMultiplayer) { toastAlpha = 1.0f; return; }
        if (GameState.currentState == State.PLAYING) {
            isTimeRunning = false; GameState.setState(State.PAUSED);
            pausePanel.setBackgroundSnapshot(gamePanel.getGameSnapshot());
            pausePanel.loadAllThumbnails(); showPanel(pausePanel);
        }
    }

    public void resumeGame() {
        if (GameState.currentState == State.PAUSED) {
            isTimeRunning = true; GameState.setState(State.PLAYING); showPanel(gamePanel);
        }
    }

    public void exitToMenu() {
        isTimeRunning = false; GameState.setState(State.MENU);
        if (isMultiplayer && netManager != null) { netManager.closeConnection(); isMultiplayer = false; }
        menuPanel.resetMenu(); audioManager.playBGM(MENU_BGM); showPanel(menuPanel);
    }

    // =========================================================
    // NHÓM 3: XỬ LÝ CHUỘT & DI CHUYỂN
    // =========================================================
    private void handleMouseInput() {
        if (isMultiplayer && currentColor != playerColor) return;
        updateCursorState();
        if (!mouse.released) return;
        int col = mouse.x / Board.SQUARE_SIZE, row = mouse.y / Board.SQUARE_SIZE;
        if (isMultiplayer && playerColor == BLACK) { col = 7 - col; row = 7 - row; }

        if (activeP == null) {
            for (Piece p : simPieces) {
                if (p.color == currentColor && p.col == col && p.row == row) {
                    activeP = p; calculateValidMoves(activeP); isClickedToMove = true; break;
                }
            }
        } else {
            boolean valid = false;
            int oldCol = activeP.col, oldRow = activeP.row;
            for (int[] mv : validMoves) if (mv[0] == col && mv[1] == row) { valid = true; break; }

            if (valid) {
                Piece captured = activeP.gettingHitP(col, row);
                simulateClickToMove(col, row);

                if (canPromote()) {
                    setPromoPieces(); promotion = true;
                } else {
                    activeP.finishMove(); copyPieces(simPieces, pieces);
                    if (isMultiplayer) netManager.sendMove(new MovePacket(oldCol, oldRow, col, row, -1));
                    playMoveSound(captured != null); checkGameEndConditions();
                }
            } else cancelOrSwitchSelection(col, row);
        }
        mouse.released = false;
    }

    public void finalizeTurn() {
        isClickedToMove = false; activeP = null; validMoves.clear(); castlingP = null;
        currentColor = (currentColor == WHITE) ? BLACK : WHITE;
        for (Piece p : pieces) if (p.color == currentColor) p.twoStepped = false;
        isKingInCheck(); resetTime(); isTimeRunning = true;
    }

    public void simulateClickToMove(int tc, int tr) {
        copyPieces(pieces, simPieces);
        activeP.canMove(tc, tr);
        if (activeP.hittingP != null) simPieces.remove(activeP.hittingP);
        activeP.col = tc; activeP.row = tr;
    }

    // =========================================================
    // NHÓM 4: MULTIPLAYER LOGIC
    // =========================================================
    public void setupMultiplayer(boolean host, int selectedColor, String ip) {
        this.isMultiplayer = true; this.isServer = host; this.playerColor = selectedColor;
        this.netManager = new NetworkManager(this);
        if (host) netManager.hostGame(5555); else netManager.joinGame(ip, 5555);
    }

    public void receiveNetworkMove(MovePacket packet) {
        if (packet.oldCol == -1) { handleRematchReceived(); return; }
        if (packet.oldCol == -2) { startNewGame(); return; }

        for (Piece p : simPieces) {
            if (p.col == packet.oldCol && p.row == packet.oldRow) {
                activeP = p;
                Piece captured = activeP.gettingHitP(packet.newCol, packet.newRow);
                simulateClickToMove(packet.newCol, packet.newRow);

                if (packet.promotionType != -1) replacePawnAndFinishNetwork(packet.promotionType);
                else activeP.finishMove();

                copyPieces(simPieces, pieces);
                playMoveSound(captured != null);
                checkGameEndConditions();
                break;
            }
        }
    }

    private void replacePawnAndFinishNetwork(int type) {
        Piece newP;
        switch (type) {
            case 1: newP = new Rook(activeP.color, activeP.col, activeP.row); break;
            case 2: newP = new Knight(activeP.color, activeP.col, activeP.row); break;
            case 3: newP = new Bishop(activeP.color, activeP.col, activeP.row); break;
            default: newP = new Queen(activeP.color, activeP.col, activeP.row); break;
        }
        newP.image = reloadPieceImage(newP);
        simPieces.add(newP); simPieces.remove(activeP);
    }

    public String getLocalIP() {
        try { return InetAddress.getLocalHost().getHostAddress(); }
        catch (Exception e) { return "127.0.0.1"; }
    }

    // =========================================================
    // NHÓM 5: SAVE / LOAD LOGIC
    // =========================================================
    private String getBaseSavePath() {
        return System.getProperty("user.dir") + File.separator + "saves";
    }

    private File getSaveFileHandle(String fileName) {
        File directory = new File(getBaseSavePath());
        if (!directory.exists()) directory.mkdirs();
        return new File(directory, fileName);
    }

    public void saveGame(int s, BufferedImage img) {
        if (isMultiplayer) return;
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(getSaveFileHandle("save_" + s + ".dat")));
            oos.writeObject(new SaveData(new ArrayList<>(this.pieces), currentColor, timeLeft)); oos.close();
            if (img != null) ImageIO.write(img, "png", getSaveFileHandle("thumb_" + s + ".png"));
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void loadGame(int s) {
        File f = getSaveFileHandle("save_" + s + ".dat");
        if (!f.exists()) return;
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
            SaveData d = (SaveData) ois.readObject(); pieces.clear(); pieces.addAll(d.pieces);
            currentColor = d.currentColor; timeLeft = d.timeLeft; ois.close();
            for (Piece p : pieces) { p.image = reloadPieceImage(p); p.updatePosition(); }
            copyPieces(pieces, simPieces); startNewGameFromLoad();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public BufferedImage getSlotThumbnail(int s) {
        try { return ImageIO.read(getSaveFileHandle("thumb_" + s + ".png")); } catch (Exception e) { return null; }
    }

    public String getSlotMetadata(int s) {
        File f = getSaveFileHandle("save_" + s + ".dat");
        if (!f.exists()) return "Empty";
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
            SaveData d = (SaveData) ois.readObject(); ois.close(); return d.saveTime;
        } catch (Exception e) { return "Empty"; }
    }

    private void startNewGameFromLoad() {
        gamePanel = new GamePanel(this);
        showPanel(gamePanel);
        audioManager.playBGM(GAME_BGM);
        GameState.setState(State.PLAYING);
        isTimeRunning = true;
        if (gameThread == null || !gameThread.isAlive()) {
            gameThread = new Thread(this); gameThread.start();
        }
    }

    // =========================================================
    // NHÓM 6: QUY TẮC CỜ (RULES)
    // =========================================================
    private void checkGameEndConditions() {
        if (isKingInCheck() && isCheckMate()) triggerEndGame(false, (currentColor == WHITE ? BLACK : WHITE));
        else if (isStaleMate()) triggerEndGame(true, null);
        else finalizeTurn();
    }

    public boolean isInsufficientMaterial() {
        for (Piece p : simPieces) if (p.type == Type.PAWN || p.type == Type.ROOK || p.type == Type.QUEEN) return false;
        return simPieces.size() <= 3;
    }

    private boolean isCheckMate() {
        if (!isKingInCheck()) return false;
        for (Piece p : simPieces) if (p.color == currentColor)
            for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++)
                if (p.canMove(c, r) && simulateMoveAndKingSafe(p, c, r)) return false;
        return true;
    }

    public boolean isKingInCheck() {
        Piece king = getKing(false); if (king == null) return false;
        for (Piece p : simPieces) if (p.color != king.color && p.canMove(king.col, king.row)) { checkingP = p; return true; }
        checkingP = null; return false;
    }

    private boolean simulateMoveAndKingSafe(Piece p, int tc, int tr) {
        int oR = p.row, oC = p.col; Piece cap = p.gettingHitP(tc, tr);
        if (cap != null) simPieces.remove(cap);
        p.col = tc; p.row = tr; boolean safe = !opponentsCanCaptureKing();
        p.col = oC; p.row = oR; if (cap != null) simPieces.add(cap);
        return safe;
    }

    private boolean opponentsCanCaptureKing() {
        Piece king = getKing(false); if (king == null) return false;
        for (Piece p : simPieces) if (p.color != king.color && p.canMove(king.col, king.row)) return true;
        return false;
    }

    private boolean isStaleMate() {
        if (isKingInCheck()) return false;
        for (Piece p : simPieces) if (p.color == currentColor)
            for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++)
                if (p.canMove(c, r) && simulateMoveAndKingSafe(p, c, r)) return false;
        return true;
    }

    private void triggerEndGame(boolean d, Integer w) {
        gameOver = true; isTimeRunning = false; isDraw = d;
        GameState.currentState = State.GAME_OVER;
        if (d) audioManager.playSFX("/audio/sfx/draw.wav");
        else audioManager.playSFX(w == WHITE ? "/audio/sfx/white_win.wav" : "/audio/sfx/black_win.wav");
    }

    // =========================================================
    // NHÓM 7: HELPERS & PROMOTION
    // =========================================================
    public boolean canPromote() {
        if (activeP == null || activeP.type != Type.PAWN) return false;
        return (activeP.color == WHITE && activeP.row == 0) || (activeP.color == BLACK && activeP.row == 7);
    }

    private void setPieces() { pieces.clear(); ChessSetupUtility.setupStandardGame(pieces); for (Piece p : pieces) { p.image = reloadPieceImage(p); p.updatePosition(); } }
    public void copyPieces(CopyOnWriteArrayList<Piece> s, CopyOnWriteArrayList<Piece> t) { t.clear(); t.addAll(s); }
    private BufferedImage reloadPieceImage(Piece p) { String pr = (p.color == WHITE) ? "w" : "b"; String n = p.type.toString().substring(0, 1).toUpperCase() + p.type.toString().substring(1).toLowerCase(); return p.getImage("/piece/" + pr + n); }
    private void calculateValidMoves(Piece p) { validMoves.clear(); for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) if (p.canMove(c, r) && simulateMoveAndKingSafe(p, c, r)) validMoves.add(new int[]{c, r, (p.gettingHitP(c, r) != null ? 1 : 0)}); }
    private void playMoveSound(boolean cap) { audioManager.playSFX(cap ? "/audio/sfx/capture.wav" : "/audio/sfx/move.wav"); new Timer(200, e -> { if (isKingInCheck() && !gameOver) audioManager.playSFX("/audio/sfx/check.wav"); }).start(); }
    private void updateCursorState() { int c = mouse.x / Board.SQUARE_SIZE, r = mouse.y / Board.SQUARE_SIZE; boolean h = false; if (activeP == null) { for (Piece p : simPieces) if (p.color == currentColor && p.col == c && p.row == r) h = true; } else { for (int[] mv : validMoves) if (mv[0] == c && mv[1] == r) h = true; } window.setCursor(Cursor.getPredefinedCursor(h ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR)); }
    private void cancelOrSwitchSelection(int c, int r) { activeP = null; isClickedToMove = false; validMoves.clear(); for (Piece p : simPieces) if (p.color == currentColor && p.col == c && p.row == r) { activeP = p; calculateValidMoves(activeP); isClickedToMove = true; break; } }
    private void setPromoPieces() { promoPieces.clear(); promoPieces.add(new Queen(currentColor, activeP.col, activeP.row)); promoPieces.add(new Rook(currentColor, activeP.col, activeP.row)); promoPieces.add(new Knight(currentColor, activeP.col, activeP.row)); promoPieces.add(new Bishop(currentColor, activeP.col, activeP.row)); for (Piece p : promoPieces) p.image = reloadPieceImage(p); }

    private void replacePawnAndFinish(Piece p) {
        int type = 0; if (p instanceof Rook) type = 1; else if (p instanceof Knight) type = 2; else if (p instanceof Bishop) type = 3;
        int oC = activeP.col, oR = activeP.row;
        p.image = reloadPieceImage(p); p.updatePosition();
        simPieces.add(p); simPieces.remove(activeP); copyPieces(simPieces, pieces);
        if (isMultiplayer) netManager.sendMove(new MovePacket(oC, oR, p.col, p.row, type));
        promotion = false; playMoveSound(false); checkGameEndConditions();
    }

    private void promoting() { if (mouse.pressed) { for (Piece p : promoPieces) if (mouse.x/Board.SQUARE_SIZE == p.col && mouse.y/Board.SQUARE_SIZE == p.row) { replacePawnAndFinish(p); break; } } }
    private void showPanel(JPanel panel) { window.showPanel(panel); }
    public void resetTime() { timeLeft = 10; lastSecond = System.currentTimeMillis(); }

    // =========================================================
    // NHÓM 8: GETTERS (XUẤT DỮ LIỆU)
    // =========================================================
    public AudioManager getAudioManager() { return audioManager; }
    public MenuPanel getMenuPanel() { return menuPanel; }
    public Piece getKing(boolean opp) { int c = opp ? (currentColor == WHITE ? BLACK : WHITE) : currentColor; for (Piece p : simPieces) if (p.type == Type.KING && p.color == c) return p; return null; }
    public int getDisplayCol(int col) { return (isMultiplayer && playerColor == BLACK) ? 7 - col : col; }
    public int getDisplayRow(int row) { return (isMultiplayer && playerColor == BLACK) ? 7 - row : row; }
    public int getCurrentColor() { return currentColor; }
    public boolean isGameOver() { return gameOver; }
    public boolean isDraw() { return isDraw; }
    public boolean isPromotion() { return promotion; }
    public boolean isClickedToMove() { return isClickedToMove; }
    public int getTimeLeft() { return timeLeft; }
    public Piece getActiveP() { return activeP; }
    public Piece getCheckingP() { return checkingP; }
    public Board getBoard() { return board; }
    public CopyOnWriteArrayList<Piece> getSimPieces() { return simPieces; }
    public ArrayList<Piece> getPromoPieces() { return promoPieces; }
    public ArrayList<int[]> getValidMoves() { return validMoves; }
    public void handleMouseRelease(int x, int y) { mouse.released = true; mouse.x = x; mouse.y = y; }
    public float getToastAlpha() { return toastAlpha; }
    public void onConfigReceived(GameConfigPacket p) { if (!isServer) { playerColor = (p.hostColor == WHITE) ? BLACK : WHITE; SwingUtilities.invokeLater(this::startNewGame); } }
}