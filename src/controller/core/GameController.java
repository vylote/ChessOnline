package controller.core;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.*;
import model.*;
import utility.*;
import view.*;
import controller.ChessSetupUtility;
import controller.Mouse;
import controller.engine.ChessLogicEngine;
import controller.integration.StockfishHandler;

public class GameController implements Runnable {
    // --- 1. CONSTANTS & UI ELEMENTS (Sửa lỗi board/WHITE/BLACK) ---
    public static final int WHITE = 0, BLACK = 1;
    public final MainFrame window;
    private GamePanel gamePanel;
    private final MenuPanel menuPanel;
    private final PausePanel pausePanel;
    private final Board board = new Board();
    private final AudioManager audioManager = new AudioManager();
    private Thread gameThread;

    // --- 2. GAME DATA ---
    public static CopyOnWriteArrayList<Piece> simPieces = new CopyOnWriteArrayList<>();
    public final CopyOnWriteArrayList<Piece> pieces = new CopyOnWriteArrayList<>();
    public final ArrayList<int[]> validMoves = new ArrayList<>();
    public final ArrayList<Piece> promoPieces = new ArrayList<>();
    public Mouse mouse = new Mouse();
    private Piece activeP, checkingP;
    public static Piece castlingP;

    // --- 3. STATE ---
    public int currentColor = WHITE, playerColor = WHITE, timeLeft = 20;
    public boolean isMultiplayer, isServer, gameOver, isDraw, isAiThinking, isTimeRunning, isClickedToMove, promotion;
    private long lastSecond = System.currentTimeMillis();
    public float toastAlpha = 0;
    public NetworkManager netManager;
    private String myName = "PC Player";
    private PlayerProfile opponentProfile;

    // --- 4. DELEGATES (Các lớp não bộ) ---
    private final ChessLogicEngine engine = new ChessLogicEngine();
    private final StockfishHandler aiHandler = new StockfishHandler(this);
    private final PersistenceService persistence = new PersistenceService();
    private final MultiplayerHandler netSync = new MultiplayerHandler(this);

    public GameController() {
        this.window = new MainFrame();
        this.menuPanel = new MenuPanel(this, window);
        this.pausePanel = new PausePanel(this, window);
        exitToMenu(); setPieces(); copyPieces(pieces, simPieces);
    }

    // =========================================================
    // NHÓM 1: FIX LỖI NetworkManager & StockfishHandler
    // =========================================================

    public void onOpponentConnected() {
        if (isServer) netManager.sendConfig(new GameConfigPacket(this.playerColor, this.myName));
        window.showPanel(new LobbyPanel(this, myName, playerColor));
    }

    public void receiveNetworkMove(MovePacket p) { netSync.processMove(p); }

    public void onConfigReceived(GameConfigPacket p) { netSync.processConfig(p); }

    public void makeAiMove(String uci) {
        if (uci == null || uci.length() < 4) return;
        int sc = uci.charAt(0)-'a', sr = 8-Character.getNumericValue(uci.charAt(1));
        activeP = simPieces.stream().filter(p -> p.col == sc && p.row == sr).findFirst().orElse(null);
        if (activeP != null) executeMove(uci.charAt(2)-'a', 8-Character.getNumericValue(uci.charAt(3)), true);
    }

    public void setMyProfile(String n, int c) { this.myName = n; this.playerColor = c; }

    // =========================================================
    // NHÓM 2: ĐIỀU PHỐI DI CHUYỂN & KẾT THÚC (FULL LOGIC)
    // =========================================================
    public void executeMove(int tc, int tr, boolean isExternal) {
        boolean isCapture = (activeP.gettingHitP(tc, tr) != null);
        isClickedToMove = false; validMoves.clear();
        simulateClickToMove(tc, tr);

        if (canPromote()) {
            if (isExternal) replacePawnAndFinishNetwork(0);
            else { promotion = true; return; }
        } else activeP.finishMove();

        if (isMultiplayer && !isExternal && netManager != null) {
            netManager.sendMove(new MovePacket(activeP.preCol, activeP.preRow, tc, tr, -1));
        }
        processGameStatus(isCapture);
    }

    private void processGameStatus(boolean isCapture) {
        boolean check = isKingInCheck();
        updateCheckingPiece(currentColor == 0 ? 1 : 0);
        playMoveSound(isCapture, castlingP != null, check);

        if (check) {
            if (engine.isCheckMate(simPieces, currentColor)) triggerEndGame(false, (currentColor == 0 ? 1 : 0));
            else { toastAlpha = 1.0f; finalizeTurn(); }
        } else {
            if (engine.isStaleMate(simPieces, currentColor)) triggerEndGame(true, null);
            else finalizeTurn();
        }
    }

    public void finalizeTurn() {
        copyPieces(simPieces, pieces);
        isClickedToMove = false; activeP = null; validMoves.clear(); castlingP = null; promotion = false;
        currentColor = (currentColor == WHITE) ? BLACK : WHITE;
        resetTime();
        if (!isMultiplayer && currentColor == BLACK && !gameOver) aiHandler.startThinking(getFEN());
    }

    // =========================================================
    // NHÓM 3: VÒNG ĐỜI (FIX PANEL) & GETTERS
    // =========================================================
    public void startNewGame() {
        pieces.clear(); ChessSetupUtility.setupStandardGame(pieces); copyPieces(pieces, simPieces);
        resetState(); gamePanel = new GamePanel(this); window.showPanel(gamePanel);
        audioManager.playBGM("/audio/bgm/game_theme.wav"); isTimeRunning = true;
        GameState.currentState = State.PLAYING;
        if (gameThread == null || !gameThread.isAlive()) { gameThread = new Thread(this); gameThread.start(); }
    }

    public void pauseGame() {
        if (isMultiplayer) { toastAlpha = 1.0f; return; }
        isTimeRunning = false; GameState.setState(State.PAUSED);
        pausePanel.setBackgroundSnapshot(gamePanel.getGameSnapshot());
        pausePanel.loadAllThumbnails(); window.showPanel(pausePanel);
    }

    public void resumeGame() { isTimeRunning = true; GameState.setState(State.PLAYING); window.showPanel(gamePanel); }
    public void exitToMenu() { isTimeRunning = false; GameState.setState(State.MENU); if (netManager != null) { netManager.closeConnection(); netManager = null; } isMultiplayer = false; menuPanel.resetMenu(); audioManager.playBGM("/audio/bgm/menu_theme.wav"); window.showPanel(menuPanel); }
    public void loadGame(int s) { SaveData d = persistence.load(s); if (d != null) { pieces.clear(); pieces.addAll(d.pieces); currentColor = d.currentColor; timeLeft = d.timeLeft; pieces.forEach(p -> { p.image = reloadPieceImage(p); p.updatePosition(); }); copyPieces(pieces, simPieces); startNewGameFromLoad(); } }
    public void saveGame(int s, BufferedImage img) { persistence.save(s, new ArrayList<>(pieces), currentColor, timeLeft, img); }

    // --- GETTERS & SETTERS (FIX LỖI MULTIPLAYERHANDLER) ---
    public void setActiveP(Piece p) { this.activeP = p; }
    public void setOpponentProfile(PlayerProfile p) { this.opponentProfile = p; }
    public PlayerProfile getOpponentProfile() { return opponentProfile; }
    public String getMyName() { return myName; }
    public int getDisplayCol(int c) { return (isMultiplayer && playerColor == BLACK) ? 7-c : c; }
    public int getDisplayRow(int r) { return (isMultiplayer && playerColor == BLACK) ? 7-r : r; }
    public Piece getKing(boolean opp) { int c = opp ? (currentColor == 0 ? 1 : 0) : currentColor; return simPieces.stream().filter(p -> p.type == Type.KING && p.color == c).findFirst().orElse(null); }
    public boolean isKingInCheck() { boolean ck = engine.isKingInCheck(simPieces, currentColor); if (ck) updateCheckingPiece(currentColor); return ck; }
    public Board getBoard() { return board; }
    public Piece getActiveP() { return activeP; }
    public ArrayList<int[]> getValidMoves() { return validMoves; }
    public int getCurrentColor() { return currentColor; }
    public int getTimeLeft() { return timeLeft; }
    public float getToastAlpha() { return toastAlpha; }
    public boolean isGameOver() { return gameOver; }
    public boolean isDraw() { return isDraw; }
    public CopyOnWriteArrayList<Piece> getSimPieces() { return simPieces; }
    public AudioManager getAudioManager() { return audioManager; }
    public ArrayList<Piece> getPromoPieces() { return promoPieces; }
    public BufferedImage getSlotThumbnail(int s) { return persistence.getThumbnail(s); }
    public String getSlotMetadata(int s) { return persistence.getMetadata(s); }
    public boolean isClickedToMove() { return isClickedToMove; }
    public boolean isPromotion() { return promotion; }
    public Piece getCheckingP() { return checkingP; }

    // --- TIỆN ÍCH HỆ THỐNG ---
    @Override public void run() { double interval = 1000000000.0/60; double delta = 0; long lastTime = System.nanoTime(); while (gameThread != null) { long now = System.nanoTime(); delta += (now - lastTime)/interval; lastTime = now; if (delta >= 1) { update(); if (gamePanel != null) gamePanel.repaint(); delta--; } } }
    private void update() { if (gameOver || GameState.currentState != State.PLAYING) return; if (toastAlpha > 0) toastAlpha -= 0.016f; long now = System.currentTimeMillis(); if (isTimeRunning && now - lastSecond >= 1000) { timeLeft--; lastSecond = now; if (timeLeft <= 0) handleTimeOut(); } if (promotion) promoting(); else handleMouseInput(); }
    public void setPieces() { pieces.clear(); ChessSetupUtility.setupStandardGame(pieces); pieces.forEach(p -> { p.image = reloadPieceImage(p); p.updatePosition(); }); }
    public void playMoveSound(boolean cap, boolean cas, boolean ck) { if (ck) audioManager.playSFX("/audio/sfx/check.wav"); else if (cas) audioManager.playSFX("/audio/sfx/castle.wav"); else if (cap) audioManager.playSFX("/audio/sfx/capture.wav"); else audioManager.playSFX("/audio/sfx/move.wav"); }
    public void setupMultiplayer(boolean h, int c, String ip) { this.isMultiplayer = true; this.isServer = h; this.playerColor = c; this.netManager = new NetworkManager(this); if (h) netManager.hostGame(5555); else netManager.joinGame(ip, 5555); }
    public void requestRematch() { if (!isMultiplayer) startNewGame(); else if (netManager != null) netManager.sendMove(new MovePacket(-1,-1,-1,-1,-1)); }
    public void hostPressStart() { if (isServer && netManager != null) { netManager.sendMove(new MovePacket(-2, -2, -2, -2, -1)); startNewGame(); } }
    public void calculateValidMoves(Piece p) { engine.calculateValidMoves(p, simPieces, validMoves, currentColor); }
    public boolean canPromote() { return activeP != null && activeP.type == Type.PAWN && (activeP.row == 0 || activeP.row == 7); }
    public void setPromoPieces() { promoPieces.clear(); int color = activeP.color; promoPieces.add(new Queen(color, 0, activeP.col)); promoPieces.add(new Rook(color, 1, activeP.col)); promoPieces.forEach(p -> p.image = reloadPieceImage(p)); }
    public void promoting() { if (mouse.released) { int c = mouse.x/Board.SQUARE_SIZE, r = mouse.y/Board.SQUARE_SIZE; for (Piece p : promoPieces) if (p.col == c && p.row == r) { replacePawnAndFinishNetwork(0); promotion = false; break; } mouse.released = false; } }
    public void handleMouseRelease(int x, int y) { mouse.released = true; mouse.x = x; mouse.y = y; }
    public void resetTime() { timeLeft = 20; lastSecond = System.currentTimeMillis(); }
    public void copyPieces(CopyOnWriteArrayList<Piece> s, CopyOnWriteArrayList<Piece> t) { t.clear(); t.addAll(s); }
    public void triggerEndGame(boolean d, Integer w) { gameOver = true; isTimeRunning = false; isDraw = d; GameState.setState(State.GAME_OVER); audioManager.playSFX(d ? "/audio/sfx/draw.wav" : "/audio/sfx/game_over.wav"); }
    public void handleTimeOut() { if (gameOver) return; if (isKingInCheck()) triggerEndGame(false, (currentColor==0?1:0)); else { toastAlpha = 1.0f; if (isMultiplayer) netManager.sendMove(new MovePacket(-4,-4,-4,-4,-1)); finalizeTurn(); } }
    public void replacePawnAndFinishNetwork(int t) { Piece newP = new Queen(activeP.color, activeP.row, activeP.col); newP.image = reloadPieceImage(newP); simPieces.add(newP); simPieces.remove(activeP); copyPieces(simPieces, pieces); processGameStatus(false); }
    private void handleMouseInput() { if (isAiThinking || (isMultiplayer && currentColor != playerColor)) return; updateCursorState(); if (!mouse.released) return; int col = mouse.x / Board.SQUARE_SIZE, row = mouse.y / Board.SQUARE_SIZE; if (isMultiplayer && playerColor == BLACK) { col = 7 - col; row = 7 - row; } final int tc = col, tr = row; if (activeP == null) { for (Piece p : simPieces) if (p.color == currentColor && p.col == tc && p.row == tr) { activeP = p; calculateValidMoves(activeP); isClickedToMove = true; break; } } else { if (validMoves.stream().anyMatch(mv -> mv[0] == tc && mv[1] == tr)) executeMove(tc, tr, false); else cancelOrSwitchSelection(tc, tr); } mouse.released = false; }
    public void updateCursorState() { int c = mouse.x/Board.SQUARE_SIZE, r = mouse.y/Board.SQUARE_SIZE; boolean h = simPieces.stream().anyMatch(p -> p.color == currentColor && p.col == c && p.row == r); window.setCursor(Cursor.getPredefinedCursor(h?Cursor.HAND_CURSOR:Cursor.DEFAULT_CURSOR)); }
    private void updateCheckingPiece(int color) { Piece k = getKing(false); checkingP = null; if (k != null) for (Piece p : simPieces) if (p.color != color && p.canMove(k.col, k.row)) { checkingP = p; break; } }
    private void cancelOrSwitchSelection(int c, int r) { activeP = null; isClickedToMove = false; validMoves.clear(); for (Piece p : simPieces) if (p.color == currentColor && p.col == c && p.row == r) { activeP = p; calculateValidMoves(activeP); isClickedToMove = true; break; } }
    public void simulateClickToMove(int tc, int tr) { castlingP = null; activeP.canMove(tc, tr); if (castlingP != null) { if (castlingP.col > activeP.col) castlingP.col = tc - 1; else castlingP.col = tc + 1; castlingP.finishMove(); } Piece hit = activeP.gettingHitP(tc, tr); if (hit != null && hit.type != Type.KING) simPieces.remove(hit); activeP.col = tc; activeP.row = tr; activeP.updatePosition(); copyPieces(simPieces, pieces); }
    private void startNewGameFromLoad() { gamePanel = new GamePanel(this); window.showPanel(gamePanel); isTimeRunning = true; GameState.setState(State.PLAYING); if (gameThread==null||!gameThread.isAlive()){gameThread=new Thread(this);gameThread.start();} }
    private void resetState() { currentColor = 0; gameOver = false; isDraw = false; isClickedToMove = false; promotion = false; resetTime(); }
    private BufferedImage reloadPieceImage(Piece p) { String pr = (p.color == 0) ? "w" : "b"; String n = p.type.toString().substring(0, 1).toUpperCase() + p.type.toString().substring(1).toLowerCase(); return p.getImage("/piece/" + pr + n); }
    public String getFEN() { StringBuilder sb = new StringBuilder(); for (int r = 0; r < 8; r++) { int e = 0; for (int c = 0; c < 8; c++) { final int finalC = c; final int finalR = r; Piece p = simPieces.stream().filter(pc -> pc.col == finalC && pc.row == finalR).findFirst().orElse(null); if (p == null) e++; else { if (e > 0) sb.append(e); sb.append(p.getFENChar()); e = 0; } } if (e > 0) sb.append(e); if (r < 7) sb.append("/"); } sb.append(currentColor == 0 ? " w " : " b ").append("KQkq - 0 1"); return sb.toString(); }
}