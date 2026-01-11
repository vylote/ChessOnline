package controller;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.imageio.ImageIO;
import javax.swing.*;

import model.*;
import utility.*;
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
    public static final int WHITE = 0, BLACK = 1;
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
    private boolean promotion, gameOver, isDraw, isAiThinking;
    public boolean isTimeRunning = false, isClickedToMove = false;
    private float toastAlpha = 0;
    private int timeLeft = 20;
    private long lastSecond = System.currentTimeMillis();

    // --- 4. FIELDS: MULTIPLAYER & AI ---
    public boolean isMultiplayer = false, isServer = false;
    public int playerColor = WHITE;
    public NetworkManager netManager;
    private String myName = "PC Player";
    private PlayerProfile opponentProfile;
    private StockfishClient sfClient;

    public GameController() {
        this.audioManager = new AudioManager();
        this.sfClient = new StockfishClient();
        // CPU i7-10750H hỗ trợ BMI2 tốt nhất cho Stockfish
        this.sfClient.startEngine("engines/stockfish.exe");

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
    // NHÓM 1: GETTERS (Sửa tất cả lỗi "Cannot find symbol" trong View)
    // =========================================================
    public boolean isClickedToMove() {
        return isClickedToMove;
    }

    public Piece getCheckingP() {
        return checkingP;
    }

    public Board getBoard() {
        return board;
    }

    public Piece getActiveP() {
        return activeP;
    }

    public ArrayList<int[]> getValidMoves() {
        return validMoves;
    }

    public boolean isPromotion() {
        return promotion;
    }

    public ArrayList<Piece> getPromoPieces() {
        return promoPieces;
    }

    public int getCurrentColor() {
        return currentColor;
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    public float getToastAlpha() {
        return toastAlpha;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean isDraw() {
        return isDraw;
    }

    public String getMyName() {
        return myName;
    }

    public PlayerProfile getOpponentProfile() {
        return opponentProfile;
    }

    public CopyOnWriteArrayList<Piece> getSimPieces() {
        return simPieces;
    }

    public AudioManager getAudioManager() {
        return audioManager;
    }

    public int getDisplayCol(int col) {
        return (isMultiplayer && playerColor == BLACK) ? 7 - col : col;
    }

    public int getDisplayRow(int row) {
        return (isMultiplayer && playerColor == BLACK) ? 7 - row : row;
    }

    // =========================================================
    // NHÓM 2: VÒNG ĐỜI & ĐIỀU HƯỚNG
    // =========================================================
    public void startNewGame() {
        pieces.clear();
        simPieces.clear();
        setPieces();
        copyPieces(pieces, simPieces);
        currentColor = WHITE;
        gameOver = false;
        isDraw = false;
        isClickedToMove = false;
        promotion = false;
        isAiThinking = false;
        activeP = null;
        checkingP = null;
        validMoves.clear();
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

    public void pauseGame() {
        if (isMultiplayer) {
            toastAlpha = 1.0f;
            return;
        }
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
        if (netManager != null) netManager.closeConnection();
        isMultiplayer = false;
        isServer = false;
        opponentProfile = null;
        menuPanel.resetMenu();
        audioManager.playBGM(MENU_BGM);
        showPanel(menuPanel);
    }

    // =========================================================
    // NHÓM 3: XỬ LÝ DI CHUYỂN & ÂM THANH (FIX LỖI ÂM THANH & TÔ MÀU)
    // =========================================================
    private void executeMove(int tc, int tr, boolean isNetwork) {
        int oc = activeP.col, or = activeP.row;
        boolean isCapture = (activeP.gettingHitP(tc, tr) != null);
        isClickedToMove = false;
        validMoves.clear();
        simulateClickToMove(tc, tr);

        if (canPromote()) {
            if (isNetwork) replacePawnAndFinishNetwork(0);
            else {
                setPromoPieces();
                promotion = true;
                return;
            }
        } else activeP.finishMove();

        if (isMultiplayer && !isNetwork) netManager.sendMove(new MovePacket(oc, or, tc, tr, -1));
        checkGameEndConditions(isCapture);
    }

    private void checkGameEndConditions(boolean isCapture) {
        boolean check = isKingInCheck();
        playMoveSound(isCapture, castlingP != null, check);

        if (check) {
            if (isCheckMate()) triggerEndGame(false, (currentColor == WHITE ? BLACK : WHITE));
            else {
                toastAlpha = 1.0f;
                finalizeTurn();
            }
        } else {
            if (isStaleMate()) triggerEndGame(true, null);
            else finalizeTurn();
        }
    }

    public void finalizeTurn() {
        copyPieces(simPieces, pieces);

        // RESET HIỂN THỊ: Xóa màu gợi ý và quân đang chọn
        isClickedToMove = false;
        activeP = null;
        validMoves.clear();
        castlingP = null;
        promotion = false;

        currentColor = (currentColor == WHITE) ? BLACK : WHITE;
        resetTime();

        // Offline AI: Kích hoạt nếu đến lượt Đen
        if (!isMultiplayer && currentColor == BLACK && !gameOver && !isAiThinking) {
            isAiThinking = true;
            new Thread(() -> {
                String move = sfClient.getBestMove(getFEN(), 1000);
                if (move != null) SwingUtilities.invokeLater(() -> makeAiMove(move));
                else isAiThinking = false;
            }).start();
        }
    }

    // =========================================================
    // NHÓM 4: XỬ LÝ CHUỘT (FIX LỖI CƠ CHẾ CHỌN QUÂN)
    // =========================================================
    private void handleMouseInput() {
        if (isAiThinking || (isMultiplayer && currentColor != playerColor)) return;
        if (!isMultiplayer && currentColor == BLACK) return;

        updateCursorState();
        if (mouse.released) {
            int col = mouse.x / Board.SQUARE_SIZE, row = mouse.y / Board.SQUARE_SIZE;
            if (isMultiplayer && playerColor == BLACK) {
                col = 7 - col;
                row = 7 - row;
            }

            if (activeP == null) {
                for (Piece p : simPieces)
                    if (p.color == currentColor && p.col == col && p.row == row) {
                        activeP = p;
                        calculateValidMoves(activeP);
                        isClickedToMove = true;
                        break;
                    }
            } else {
                final int targetCol = col;
                final int targetRow = row;

                if (validMoves.stream().anyMatch(mv -> mv[0] == targetCol && mv[1] == targetRow)) {
                    executeMove(targetCol, targetRow, false);
                } else cancelOrSwitchSelection(col, row);
            }
            mouse.released = false;
        }
    }

    public void calculateValidMoves(Piece p) {
        validMoves.clear();
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (p.canMove(c, r) && simulateMoveAndKingSafe(p, c, r)) {
                    // PHẢI CÓ 3 PHẦN TỬ: [cột, hàng, 0=đi/1=ăn] để GamePanel vẽ màu chuẩn
                    validMoves.add(new int[]{c, r, (p.gettingHitP(c, r) != null ? 1 : 0)});
                }
    }

    // =========================================================
    // NHÓM 5: AI & MULTIPLAYER (FIX LỖI AI ĐI LIÊN TỤC)
    // =========================================================
    public void makeAiMove(String uci) {
        if (uci == null || uci.length() < 4) {
            isAiThinking = false;
            return;
        }
        int sc = uci.charAt(0) - 'a', sr = 8 - Character.getNumericValue(uci.charAt(1));
        activeP = getPieceAt(sc, sr);
        if (activeP != null) {
            executeMove(uci.charAt(2) - 'a', 8 - Character.getNumericValue(uci.charAt(3)), true);
            isAiThinking = false;
        } else isAiThinking = false;
    }

    public void setupMultiplayer(boolean host, int color, String ip) {
        this.isMultiplayer = true;
        this.isServer = host;
        this.playerColor = color;
        this.netManager = new NetworkManager(this);
        if (host) netManager.hostGame(5555);
        else netManager.joinGame(ip, 5555);
    }

    public void hostPressStart() {
        if (isServer && netManager != null) netManager.sendMove(new MovePacket(-2, -2, -2, -2, -1));
        startNewGame();
    }

    public void onOpponentConnected() {
        if (isServer) netManager.sendConfig(new GameConfigPacket(this.playerColor, this.myName));
        showPanel(new LobbyPanel(this, myName, playerColor));
    }

    public void receiveNetworkMove(MovePacket packet) {
        // Nhận tín hiệu hết giờ từ đối thủ
        if (packet.oldCol == -4) {
            finalizeTurn();
            return;
        }

        // Các logic nhận nước đi khác giữ nguyên...
        if (packet.oldCol == -2) {
            if (!isServer) startNewGame();
            return;
        }
        for (Piece p : simPieces)
            if (p.col == packet.oldCol && p.row == packet.oldRow) {
                activeP = p;
                executeMove(packet.newCol, packet.newRow, true);
                break;
            }
    }

    public void onConfigReceived(GameConfigPacket p) {
        this.opponentProfile = new PlayerProfile(p.playerName, p.hostColor, "");
        if (!isServer) {
            this.playerColor = (p.hostColor == WHITE) ? BLACK : WHITE;
            netManager.sendConfig(new GameConfigPacket(this.playerColor, this.myName));
            LobbyPanel lp = new LobbyPanel(this, myName, playerColor);
            lp.setOpponent(opponentProfile);
            showPanel(lp);
        }
    }

    public void handleRematchReceived() {
        int res = JOptionPane.showConfirmDialog(window, "Chơi lại?", "Rematch", JOptionPane.YES_NO_OPTION);
        if (res == JOptionPane.YES_OPTION && isServer) hostPressStart();
    }

    // =========================================================
    // NHÓM 6: PERSISTENCE (SAVE/LOAD)
    // =========================================================
    public void saveGame(int slot, BufferedImage img) {
        try {
            File dir = new File("saves");
            if (!dir.exists()) dir.mkdirs();
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(dir, "save_" + slot + ".dat")));
            oos.writeObject(new SaveData(new ArrayList<>(pieces), currentColor, timeLeft));
            oos.close();
            if (img != null) ImageIO.write(img, "png", new File(dir, "thumb_" + slot + ".png"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadGame(int slot) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File("saves/save_" + slot + ".dat")));
            SaveData d = (SaveData) ois.readObject();
            pieces.clear();
            pieces.addAll(d.pieces);
            currentColor = d.currentColor;
            timeLeft = d.timeLeft;
            ois.close();
            for (Piece p : pieces) {
                p.image = reloadPieceImage(p);
                p.updatePosition();
            }
            copyPieces(pieces, simPieces);
            startNewGameFromLoad();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BufferedImage getSlotThumbnail(int s) {
        try {
            return ImageIO.read(new File("saves/thumb_" + s + ".png"));
        } catch (Exception e) {
            return null;
        }
    }

    public String getSlotMetadata(int s) {
        File f = new File("saves/save_" + s + ".dat");
        return f.exists() ? "Saved Game" : "Empty";
    }

    // =========================================================
    // NHÓM 7: HỆ THỐNG & TIỆN ÍCH (FULL SYMBOLS)
    // =========================================================
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
        if (gameOver || GameState.currentState != State.PLAYING) return;
        if (toastAlpha > 0) toastAlpha -= 0.016f;
        long now = System.currentTimeMillis();
        if (isTimeRunning && now - lastSecond >= 1000) {
            timeLeft--;
            lastSecond = now;
            if (timeLeft <= 0) handleTimeOut();
        }
        if (promotion) promoting();
        else handleMouseInput();
    }

    public void setPieces() {
        pieces.clear();
        ChessSetupUtility.setupStandardGame(pieces);
        pieces.forEach(p -> {
            p.image = reloadPieceImage(p);
            p.updatePosition();
        });
    }

    public void simulateClickToMove(int tc, int tr) {
        castlingP = null;
        activeP.canMove(tc, tr);
        if (castlingP != null) {
            if (castlingP.col > activeP.col) castlingP.col = tc - 1;
            else castlingP.col = tc + 1;
            castlingP.finishMove();
        }
        Piece hit = activeP.gettingHitP(tc, tr);
        if (hit != null && hit.type != Type.KING) simPieces.remove(hit);
        activeP.col = tc;
        activeP.row = tr;
        activeP.updatePosition();
        copyPieces(simPieces, pieces);
    }

    public boolean isKingInCheck() {
        Piece k = getKing(false);
        if (k == null) return false;
        for (Piece p : simPieces)
            if (p.color != k.color && p.canMove(k.col, k.row)) {
                checkingP = p;
                return true;
            }
        checkingP = null;
        return false;
    }

    public boolean isCheckMate() {
        return isKingInCheck() && isStaleMate();
    }

    public boolean isStaleMate() {
        return simPieces.stream().filter(p -> p.color == currentColor).noneMatch(p -> {
            for (int r = 0; r < 8; r++)
                for (int c = 0; c < 8; c++) if (p.canMove(c, r) && simulateMoveAndKingSafe(p, c, r)) return true;
            return false;
        });
    }

    public void triggerEndGame(boolean d, Integer w) {
        gameOver = true;
        isTimeRunning = false;
        isDraw = d;
        GameState.setState(State.GAME_OVER);
        audioManager.playSFX(d ? "/audio/sfx/draw.wav" : "/audio/sfx/game_over.wav");
    }

    public void resetTime() {
        timeLeft = 20;
        lastSecond = System.currentTimeMillis();
    }

    public void handleTimeOut() {
        if (gameOver) return;

        // 1. Nếu đang trong quá trình phong cấp, tự động phong Hậu và đổi lượt
        if (promotion) {
            replacePawnAndFinishNetwork(0);
            promotion = false;
            return;
        }

        // 2. Kiểm tra trạng thái chiếu tướng
        if (isKingInCheck()) {
            // Nếu bị chiếu mà hết giờ -> THUA
            triggerEndGame(false, (currentColor == WHITE ? BLACK : WHITE));
        } else {
            // Nếu KHÔNG bị chiếu -> CHỈ ĐỔI LƯỢT (Theo yêu cầu của bạn)
            toastAlpha = 1.0f; // Hiện thông báo "TIMEOUT - TURN SKIP"

            // Nếu là Online, gửi gói tin báo hiệu hết giờ để đối thủ cũng đổi lượt
            if (isMultiplayer && netManager != null) {
                // Gửi mã đặc biệt (-4) để báo hiệu Timeout Turn Skip
                netManager.sendMove(new MovePacket(-4, -4, -4, -4, -1));
            }

            finalizeTurn();
        }
    }

    public void updateCursorState() {
        int c = mouse.x / Board.SQUARE_SIZE, r = mouse.y / Board.SQUARE_SIZE;
        boolean h = simPieces.stream().anyMatch(p -> p.color == currentColor && p.col == c && p.row == r);
        window.setCursor(Cursor.getPredefinedCursor(h ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    public boolean canPromote() {
        return activeP != null && activeP.type == Type.PAWN && (activeP.row == 0 || activeP.row == 7);
    }

    public void setPromoPieces() {
        promoPieces.clear();
        int color = activeP.color;
        promoPieces.add(new Queen(color, 0, activeP.col));
        promoPieces.add(new Rook(color, 1, activeP.col));
        promoPieces.forEach(p -> p.image = reloadPieceImage(p));
    }

    public void promoting() {
        if (mouse.released) {
            int c = mouse.x / Board.SQUARE_SIZE, r = mouse.y / Board.SQUARE_SIZE;
            for (Piece p : promoPieces)
                if (p.col == c && p.row == r) {
                    replacePawnAndFinishNetwork(0);
                    promotion = false;
                    break;
                }
            mouse.released = false;
        }
    }

    public void replacePawnAndFinishNetwork(int t) {
        Piece newP = new Queen(activeP.color, activeP.row, activeP.col);
        newP.image = reloadPieceImage(newP);
        simPieces.add(newP);
        simPieces.remove(activeP);
        copyPieces(simPieces, pieces);
        checkGameEndConditions(false);
    }

    public Piece getPieceAt(int c, int r) {
        return simPieces.stream().filter(p -> p.col == c && p.row == r).findFirst().orElse(null);
    }

    public Piece getKing(boolean opp) {
        int c = opp ? (currentColor == WHITE ? BLACK : WHITE) : currentColor;
        return simPieces.stream().filter(p -> p.type == Type.KING && p.color == c).findFirst().orElse(null);
    }

    public void copyPieces(CopyOnWriteArrayList<Piece> s, CopyOnWriteArrayList<Piece> t) {
        t.clear();
        t.addAll(s);
    }

    public void handleMouseRelease(int x, int y) {
        mouse.released = true;
        mouse.x = x;
        mouse.y = y;
    }

    public void requestRematch() {
        if (!isMultiplayer) startNewGame();
        else netManager.sendMove(new MovePacket(-1, -1, -1, -1, -1));
    }

    public void setMyProfile(String n, int c) {
        this.myName = n;
        this.playerColor = c;
    }

    public String getFEN() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < 8; r++) {
            int e = 0;
            for (int c = 0; c < 8; c++) {
                Piece p = getPieceAt(c, r);
                if (p == null) e++;
                else {
                    if (e > 0) sb.append(e);
                    sb.append(p.getFENChar());
                    e = 0;
                }
            }
            if (e > 0) sb.append(e);
            if (r < 7) sb.append("/");
        }
        sb.append(currentColor == WHITE ? " w " : " b ").append("KQkq - 0 1");
        return sb.toString();
    }

    private void showPanel(JPanel p) {
        window.showPanel(p);
    }

    private void startNewGameFromLoad() {
        gamePanel = new GamePanel(this);
        showPanel(gamePanel);
        isTimeRunning = true;
        GameState.setState(State.PLAYING);
    }

    private BufferedImage reloadPieceImage(Piece p) {
        String pr = (p.color == WHITE) ? "w" : "b";
        String n = p.type.toString().substring(0, 1).toUpperCase() + p.type.toString().substring(1).toLowerCase();
        return p.getImage("/piece/" + pr + n);
    }

    private void playMoveSound(boolean cap, boolean castled, boolean check) {
        if (check) audioManager.playSFX("/audio/sfx/check.wav");
        else if (castled) audioManager.playSFX("/audio/sfx/castle.wav");
        else if (cap) audioManager.playSFX("/audio/sfx/capture.wav");
        else audioManager.playSFX("/audio/sfx/move.wav");
    }

    private boolean simulateMoveAndKingSafe(Piece p, int tc, int tr) {
        int oc = p.col, or = p.row;
        Piece hit = p.gettingHitP(tc, tr);
        if (hit != null) simPieces.remove(hit);
        p.col = tc;
        p.row = tr;
        boolean safe = !opponentsCanCaptureKing();
        p.col = oc;
        p.row = or;
        if (hit != null) simPieces.add(hit);
        return safe;
    }

    private boolean opponentsCanCaptureKing() {
        Piece k = getKing(false);
        return k != null && simPieces.stream().anyMatch(p -> p.color != k.color && p.canMove(k.col, k.row));
    }

    private void cancelOrSwitchSelection(int c, int r) {
        activeP = null;
        isClickedToMove = false;
        validMoves.clear();
        for (Piece p : simPieces)
            if (p.color == currentColor && p.col == c && p.row == r) {
                activeP = p;
                calculateValidMoves(activeP);
                isClickedToMove = true;
                break;
            }
    }
}