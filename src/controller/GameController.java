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
    public static Piece castlingP; // Quân xe dùng để nhập thành
    private final ArrayList<int[]> validMoves = new ArrayList<>();
    private final ArrayList<Piece> promoPieces = new ArrayList<>();

    // --- 3. FIELDS: TRẠNG THÁI GAME ---
    private boolean promotion, gameOver, isDraw;
    public boolean isTimeRunning = false;
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
        pieces.clear();
        simPieces.clear();
        setPieces();
        copyPieces(pieces, simPieces);
        currentColor = WHITE; // Luôn bắt đầu bằng quân Trắng
        gameOver = false;
        isDraw = false;
        isClickedToMove = false;
        promotion = false;
        activeP = null;
        checkingP = null;
        validMoves.clear();
        resetTime(); // Reset về 10s

        gamePanel = new GamePanel(this);
        showPanel(gamePanel);
        audioManager.playBGM(GAME_BGM);

        // CHỈNH SỬA: Mặc định dừng đồng hồ cho Multiplayer để đợi handshake hoàn tất
        isTimeRunning = !isMultiplayer;

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
        if (toastAlpha > 0) {
            toastAlpha -= 0.016f;
            if (toastAlpha < 0) toastAlpha = 0;
        }
        isKingInCheck();
        if (isInsufficientMaterial()) {
            triggerEndGame(true, null);
            return;
        }
        long now = System.currentTimeMillis();
        if (isTimeRunning && now - lastSecond >= 1000) {
            lastSecond = now;
            timeLeft--;
            if (timeLeft <= 0) {
                handleTimeOut();
                return;
            }
        }
        if (promotion) promoting();
        else handleMouseInput();
    }

    private void handleTimeOut() {
        if (promotion) {
            // CHỖ CẦN SỬA: Nếu đang thăng cấp mà hết giờ, tự động lấy quân đầu tiên trong danh sách
            if (!promoPieces.isEmpty()) {
                replacePawnAndFinish(promoPieces.get(0));
            } else {
                finalizeTurn();
            }
        } else if (isKingInCheck()) {
            triggerEndGame(false, (currentColor == WHITE ? BLACK : WHITE));
        } else {
            finalizeTurn();
        }
    }

    // =========================================================
    // NHÓM 2: ĐIỀU HƯỚNG & MENU (REMATCH / EXIT)
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
        if (isMultiplayer && netManager != null) {
            netManager.closeConnection();
            isMultiplayer = false;
        }
        menuPanel.resetMenu();
        audioManager.playBGM(MENU_BGM);
        showPanel(menuPanel);
    }

    // =========================================================
    // NHÓM 3: XỬ LÝ CHUỘT & DI CHUYỂN
    // =========================================================
    private void handleMouseInput() {
        if (isMultiplayer && currentColor != playerColor) return;
        updateCursorState();
        if (!mouse.released) return;
        int col = mouse.x / Board.SQUARE_SIZE, row = mouse.y / Board.SQUARE_SIZE;
        if (isMultiplayer && playerColor == BLACK) {
            col = 7 - col;
            row = 7 - row;
        }

        if (activeP == null) {
            for (Piece p : simPieces) {
                if (p.color == currentColor && p.col == col && p.row == row) {
                    activeP = p;
                    calculateValidMoves(activeP);
                    isClickedToMove = true;
                    break;
                }
            }
        } else {
            boolean valid = false;
            int oldCol = activeP.col, oldRow = activeP.row;
            for (int[] mv : validMoves)
                if (mv[0] == col && mv[1] == row) {
                    valid = true;
                    break;
                }

            if (valid) {
                Piece captured = activeP.gettingHitP(col, row);
                // Lưu tọa độ TRƯỚC KHI di chuyển (oldCol và oldRow đã có ở trên rồi)
                simulateClickToMove(col, row);

                boolean castled = (castlingP != null);

                if (canPromote()) {
                    setPromoPieces();
                    if (promoPieces.isEmpty()) {
                        activeP.finishMove();
                        copyPieces(simPieces, pieces);
                        // SỬA TẠI ĐÂY: Dùng oldCol, oldRow thay vì oC, oR
                        if (isMultiplayer) netManager.sendMove(new MovePacket(oldCol, oldRow, col, row, -1));
                        playMoveSound(captured != null, false);
                        finalizeTurn();
                    } else {
                        promotion = true;
                    }
                } else {
                    activeP.finishMove();
                    copyPieces(simPieces, pieces);
                    // Đoạn này bạn đã dùng oldCol, oldRow là đúng
                    if (isMultiplayer) netManager.sendMove(new MovePacket(oldCol, oldRow, col, row, -1));
                    playMoveSound(captured != null, castled);
                    checkGameEndConditions();
                }
            } else cancelOrSwitchSelection(col, row);
        }
        mouse.released = false;
    }

    public void finalizeTurn() {
        // 1. Chốt dữ liệu (Quan trọng để Xe và Vua ở vị trí mới)
        copyPieces(simPieces, pieces);

        // 2. Ép cập nhật tọa độ vẽ (Xử lý lỗi Ghost Xe)
        for (Piece p : pieces) {
            p.updatePosition();
        }

        // 3. Reset các biến trạng thái
        isClickedToMove = false;
        activeP = null;
        validMoves.clear();
        castlingP = null;
        promotion = false;
        promoPieces.clear();

        // 4. Đổi lượt
        currentColor = (currentColor == WHITE) ? BLACK : WHITE;

        // 5. Reset En Passant cho phe vừa đến lượt
        for (Piece p : pieces) {
            if (p.color == currentColor) p.twoStepped = false;
        }

        // 6. Reset đồng hồ
        resetTime();
        this.isTimeRunning = true;
        window.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    public void simulateClickToMove(int tc, int tr) {
        copyPieces(pieces, simPieces);
        activeP.canMove(tc, tr);
        if (castlingP != null) {
            if (castlingP.col == activeP.col + 3) castlingP.col = activeP.col + 1;
            else if (castlingP.col == activeP.col - 4) castlingP.col = activeP.col - 1;
            // QUAN TRỌNG: Cập nhật cả preCol/preRow để xóa vết cũ hoàn toàn
            castlingP.preCol = castlingP.col;
            castlingP.preRow = castlingP.row;
            castlingP.updatePosition();
        }
        if (activeP.hittingP != null) simPieces.remove(activeP.hittingP);
        activeP.col = tc;
        activeP.row = tr;
        activeP.updatePosition();

        // Đảm bảo đồng bộ lại danh sách pieces chính thức ngay sau khi Xe di chuyển
        copyPieces(simPieces, pieces);
    }

    // =========================================================
    // NHÓM 4: MULTIPLAYER LOGIC
    // =========================================================
    public void setupMultiplayer(boolean host, int selectedColor, String ip) {
        this.isMultiplayer = true;
        this.isServer = host;
        this.playerColor = selectedColor;
        this.isTimeRunning = false; // Mặc định dừng

        this.netManager = new NetworkManager(this);
        if (host) {
            netManager.hostGame(5555);
            // Khi Joiner kết nối, NetworkManager của bạn nên gọi một hàm gửi Config
            // Host sẽ bắt đầu chạy đồng hồ NGAY SAU KHI gửi xong ConfigPacket
        } else {
            netManager.joinGame(ip, 5555);
        }
    }

    public void receiveNetworkMove(MovePacket packet) {
        if (packet.oldCol == -1) { handleRematchReceived(); return; }
        if (packet.oldCol == -2) { startNewGame(); return; }

        for (Piece p : simPieces) {
            if (p.col == packet.oldCol && p.row == packet.oldRow) {
                activeP = p;

                // QUAN TRỌNG: Reset castlingP trước khi kiểm tra
                castlingP = null;

                // Gọi canMove để Piece tự động tìm và gán quân Xe vào castlingP
                activeP.canMove(packet.newCol, packet.newRow);

                // Sau đó mới thực hiện simulate di chuyển (bao gồm cả di chuyển Xe)
                simulateClickToMove(packet.newCol, packet.newRow);

                if (packet.promotionType != -1) {
                    replacePawnAndFinishNetwork(packet.promotionType);
                } else {
                    activeP.finishMove();
                }

                copyPieces(simPieces, pieces);
                playMoveSound(activeP.hittingP != null, castlingP != null);

                // Kiểm tra kết thúc game và đổi lượt
                checkGameEndConditions();
                break;
            }
        }
    }

    // CHỖ CẦN SỬA 1: Đổi thành public và đảm bảo chỉ có 1 hàm này trong file
    public void replacePawnAndFinish(Piece p) {
        int targetCol = activeP.col;
        int targetRow = activeP.row;
        int oldCol = activeP.preCol;
        int oldRow = activeP.preRow;

        p.col = targetCol;
        p.row = targetRow;
        p.updatePosition();
        p.image = reloadPieceImage(p);

        simPieces.add(p);
        simPieces.remove(activeP);
        copyPieces(simPieces, pieces);

        int type = (p instanceof Rook) ? 1 : (p instanceof Knight) ? 2 : (p instanceof Bishop) ? 3 : 0;

        if (isMultiplayer) {
            netManager.sendMove(new MovePacket(oldCol, oldRow, targetCol, targetRow, type));
        }

        promotion = false;
        // Đồng bộ kết thúc lượt
        checkGameEndConditions();
    }

    // CHỖ CẦN SỬA 2: Thêm hàm này vào GameController
    public void replacePawnAndFinishNetwork(int type) {
        Piece newP;
        int col = activeP.col;
        int row = activeP.row;
        int color = activeP.color;

        switch (type) {
            case 1: newP = new Rook(color, row, col); break;
            case 2: newP = new Knight(color, row, col); break;
            case 3: newP = new Bishop(color, row, col); break;
            default: newP = new Queen(color, row, col); break;
        }

        newP.image = reloadPieceImage(newP);
        newP.updatePosition();

        simPieces.add(newP);
        simPieces.remove(activeP);
        // Cập nhật lại danh sách chính thức
        copyPieces(simPieces, pieces);
    }

    public String getLocalIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
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
            oos.writeObject(new SaveData(new ArrayList<>(this.pieces), currentColor, timeLeft));
            oos.close();
            if (img != null) ImageIO.write(img, "png", getSaveFileHandle("thumb_" + s + ".png"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadGame(int s) {
        File f = getSaveFileHandle("save_" + s + ".dat");
        if (!f.exists()) return;
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
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
            return ImageIO.read(getSaveFileHandle("thumb_" + s + ".png"));
        } catch (Exception e) {
            return null;
        }
    }

    public String getSlotMetadata(int s) {
        File f = getSaveFileHandle("save_" + s + ".dat");
        if (!f.exists()) return "Empty";
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
            SaveData d = (SaveData) ois.readObject();
            ois.close();
            return d.saveTime;
        } catch (Exception e) {
            return "Empty";
        }
    }

    private void startNewGameFromLoad() {
        gamePanel = new GamePanel(this);
        showPanel(gamePanel);
        audioManager.playBGM(GAME_BGM);
        GameState.setState(State.PLAYING);
        isTimeRunning = true;
        if (gameThread == null || !gameThread.isAlive()) {
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    // =========================================================
    // NHÓM 6: QUY TẮC CỜ (RULES)
    // =========================================================
    private void checkGameEndConditions() {
        if (isKingInCheck()) {
            if (isCheckMate()) {
                triggerEndGame(false, (currentColor == WHITE ? BLACK : WHITE));
            } else {
                // Hiện chữ "CHECK!" mờ dần nếu bạn muốn
                toastAlpha = 1.0f;
                finalizeTurn(); // Không bị chiếu bí -> Đổi lượt
            }
        } else {
            if (isStaleMate()) {
                triggerEndGame(true, null);
            } else {
                finalizeTurn(); // Không vấn đề gì -> Đổi lượt
            }
        }
    }

    public boolean isInsufficientMaterial() {
        for (Piece p : simPieces) if (p.type == Type.PAWN || p.type == Type.ROOK || p.type == Type.QUEEN) return false;
        return simPieces.size() <= 3;
    }

    private boolean isCheckMate() {
        if (!isKingInCheck()) return false;

        for (Piece p : simPieces) {
            if (p.color == currentColor) {
                // Thay vì chạy 64 ô, ta chỉ chạy 64 ô nếu p.canMove(c, r)
                // Logic này có vẻ giống nhau nhưng ta có thể tối ưu simulateMove nội bộ
                for (int r = 0; r < 8; r++) {
                    for (int c = 0; c < 8; c++) {
                        // Nếu có bất kỳ nước đi nào hợp lệ VÀ cứu được Vua
                        if (p.canMove(c, r) && simulateMoveAndKingSafe(p, c, r)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true; // Không còn nước nào cứu được -> Chiếu bí
    }

    public boolean isKingInCheck() {
        Piece king = getKing(false);
        if (king == null) return false;
        for (Piece p : simPieces)
            if (p.color != king.color && p.canMove(king.col, king.row)) {
                checkingP = p;
                return true;
            }
        checkingP = null;
        return false;
    }

    private boolean simulateMoveAndKingSafe(Piece p, int tc, int tr) {
        int oR = p.row, oC = p.col;
        Piece cap = p.gettingHitP(tc, tr);

        Piece vRook = null;
        int vROldCol = -1;

        // 1. Kiểm tra điều kiện "Băng qua ô bị chiếu" khi Nhập thành
        if (p.type == Type.KING && Math.abs(tc - oC) == 2) {
            int intermediateCol = (tc > oC) ? oC + 1 : oC - 1;

            // Giả lập Vua đứng ở ô trung gian
            p.col = intermediateCol;
            if (opponentsCanCaptureKing()) {
                p.col = oC; // Trả về vị trí cũ trước khi thoát
                return false; // Ô trung gian bị kiểm soát -> Không được nhập thành
            }
            p.col = oC; // Trả về để thực hiện tiếp giả lập vị trí cuối

            // Tìm quân Xe để giả lập tiếp vị trí cuối (như bạn đã làm)
            for (Piece targetXe : simPieces) {
                if (targetXe.type == Type.ROOK && targetXe.color == p.color) {
                    if (tc > oC && targetXe.col == 7) vRook = targetXe;
                    else if (tc < oC && targetXe.col == 0) vRook = targetXe;
                }
            }
            if (vRook != null) {
                vROldCol = vRook.col;
                vRook.col = (tc > oC) ? tc - 1 : tc + 1;
            }
        }

        // 2. Thực hiện giả lập vị trí CUỐI cùng
        if (cap != null) simPieces.remove(cap);
        p.col = tc;
        p.row = tr;

        boolean safe = !opponentsCanCaptureKing();

        // 3. Hoàn tác (Backtracking)
        p.col = oC;
        p.row = oR;
        if (cap != null) simPieces.add(cap);
        if (vRook != null) vRook.col = vROldCol;

        return safe;
    }

    private boolean opponentsCanCaptureKing() {
        Piece king = getKing(false);
        if (king == null) return false;
        for (Piece p : simPieces) if (p.color != king.color && p.canMove(king.col, king.row)) return true;
        return false;
    }

    private boolean isStaleMate() {
        if (isKingInCheck()) return false;
        for (Piece p : simPieces)
            if (p.color == currentColor)
                for (int r = 0; r < 8; r++)
                    for (int c = 0; c < 8; c++)
                        if (p.canMove(c, r) && simulateMoveAndKingSafe(p, c, r)) return false;
        return true;
    }

    private void triggerEndGame(boolean d, Integer w) {
        gameOver = true;
        isTimeRunning = false;
        isDraw = d;
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

    private void setPieces() {
        pieces.clear();
        ChessSetupUtility.setupStandardGame(pieces);
        for (Piece p : pieces) {
            p.image = reloadPieceImage(p);
            p.updatePosition();
        }
    }

    public void copyPieces(CopyOnWriteArrayList<Piece> s, CopyOnWriteArrayList<Piece> t) {
        t.clear();
        t.addAll(s);
    }

    private BufferedImage reloadPieceImage(Piece p) {
        String pr = (p.color == WHITE) ? "w" : "b";
        String n = p.type.toString().substring(0, 1).toUpperCase() + p.type.toString().substring(1).toLowerCase();
        return p.getImage("/piece/" + pr + n);
    }

    private void calculateValidMoves(Piece p) {
        validMoves.clear();
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (p.canMove(c, r) && simulateMoveAndKingSafe(p, c, r))
                    validMoves.add(new int[]{c, r, (p.gettingHitP(c, r) != null ? 1 : 0)});
    }

    private void playMoveSound(boolean cap, boolean castled) {
        if (castled) {
            audioManager.playSFX("/audio/sfx/castle.wav");
        } else if (cap) {
            audioManager.playSFX("/audio/sfx/capture.wav");
        } else {
            audioManager.playSFX("/audio/sfx/move.wav");
        }

        // FIX: Sử dụng Timer với setRepeats(false) để không lặp âm check
        Timer checkTimer = new Timer(200, e -> {
            if (isKingInCheck() && !gameOver) {
                audioManager.playSFX("/audio/sfx/check.wav");
            }
        });
        checkTimer.setRepeats(false);
        checkTimer.start();
    }

    private void updateCursorState() {
        int c = mouse.x / Board.SQUARE_SIZE, r = mouse.y / Board.SQUARE_SIZE;
        boolean h = false;
        if (activeP == null) {
            for (Piece p : simPieces) if (p.color == currentColor && p.col == c && p.row == r) h = true;
        } else {
            for (int[] mv : validMoves) if (mv[0] == c && mv[1] == r) h = true;
        }
        window.setCursor(Cursor.getPredefinedCursor(h ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
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

    private boolean isPieceLost(Type type) {
        int count = 0;
        for (Piece p : simPieces) {
            if (p.color == currentColor && p.type == type) count++;
        }
        // Queen mặc định có 1, các quân khác mặc định có 2
        if (type == Type.QUEEN) return count < 1;
        return count < 2;
    }

    public void setPromoPieces() {
        promoPieces.clear();
        // Lấy màu trực tiếp từ quân tốt đang được chọn (activeP)
        int pColor = activeP.color;
        // Chỉ thêm vào danh sách nếu quân đó đã bị ăn mất (Cơ sở thăng cấp)
        if (isPieceLost(Type.QUEEN)) promoPieces.add(new Queen(pColor, activeP.row, activeP.col));
        if (isPieceLost(Type.ROOK)) promoPieces.add(new Rook(pColor, activeP.row, activeP.col));
        if (isPieceLost(Type.BISHOP)) promoPieces.add(new Bishop(pColor, activeP.row, activeP.col));
        if (isPieceLost(Type.KNIGHT)) promoPieces.add(new Knight(pColor, activeP.row, activeP.col));

        for (Piece p : promoPieces) p.image = reloadPieceImage(p);
    }


    private void promoting() {
        if (mouse.pressed) {
            for (Piece p : promoPieces)
                if (mouse.x / Board.SQUARE_SIZE == p.col && mouse.y / Board.SQUARE_SIZE == p.row) {
                    replacePawnAndFinish(p);
                    break;
                }
        }
    }

    private void showPanel(JPanel panel) {
        window.showPanel(panel);
    }

    public void resetTime() {
        timeLeft = 10;
        lastSecond = System.currentTimeMillis();
    }

    // =========================================================
    // NHÓM 8: GETTERS (ĐẦY ĐỦ)
    // =========================================================
    public AudioManager getAudioManager() {
        return audioManager;
    }

    public MenuPanel getMenuPanel() {
        return menuPanel;
    }

    public Piece getKing(boolean opp) {
        int c = opp ? (currentColor == WHITE ? BLACK : WHITE) : currentColor;
        for (Piece p : simPieces) if (p.type == Type.KING && p.color == c) return p;
        return null;
    }

    public int getDisplayCol(int col) {
        // Nếu tôi là quân ĐEN, tôi phải nhìn bàn cờ đảo ngược lại
        return (isMultiplayer && playerColor == BLACK) ? 7 - col : col;
    }

    public int getDisplayRow(int row) {
        return (isMultiplayer && playerColor == BLACK) ? 7 - row : row;
    }

    public int getCurrentColor() {
        return currentColor;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean isDraw() {
        return isDraw;
    }

    public boolean isPromotion() {
        return promotion;
    }

    public boolean isClickedToMove() {
        return isClickedToMove;
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    public Piece getActiveP() {
        return activeP;
    }

    public Piece getCheckingP() {
        return checkingP;
    }

    public Board getBoard() {
        return board;
    }

    public CopyOnWriteArrayList<Piece> getSimPieces() {
        return simPieces;
    }

    public ArrayList<Piece> getPromoPieces() {
        return promoPieces;
    }

    public ArrayList<int[]> getValidMoves() {
        return validMoves;
    }

    public void handleMouseRelease(int x, int y) {
        mouse.released = true;
        mouse.x = x;
        mouse.y = y;
    }

    public float getToastAlpha() {
        return toastAlpha;
    }

    // Khi Joiner nhận được Config từ Host
    public void onConfigReceived(GameConfigPacket p) {
        if (!isServer) {
            this.isMultiplayer = true;
            this.playerColor = (p.hostColor == WHITE) ? BLACK : WHITE;

            SwingUtilities.invokeLater(() -> {
                startNewGame();
                // CHỈ chạy đồng hồ khi mọi thứ đã render xong
                this.isTimeRunning = true;
                resetTime();
            });
        }
    }
}