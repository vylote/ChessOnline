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
import utility.StockfishClient;
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
    public boolean isTimeRunning = false;
    private boolean isClickedToMove = false;
    private float toastAlpha = 0;
    private int timeLeft = 20;
    private long lastSecond = System.currentTimeMillis();
    private boolean isAiThinking = false;

    // --- 4. FIELDS: MULTIPLAYER ---
    public boolean isMultiplayer = false;
    public boolean isServer = false;
    public int playerColor = WHITE;
    public NetworkManager netManager;
    private String myName = "PC Player";
    private PlayerProfile opponentProfile;

    private StockfishClient sfClient;

    public GameController() {
        this.audioManager = new AudioManager();
        this.sfClient = new StockfishClient(); // Khởi tạo Client
        // Chỉ dùng 1 đường dẫn chuẩn nhất
        String path = "engines/stockfish.exe";
        boolean success = this.sfClient.startEngine(path);

        if (!success) {
            System.err.println("AI không thể khởi động!");
        }
        // Đường dẫn file exe bạn đã tải
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
    // NHÓM 1: GETTERS (Không thay đổi)
    // =========================================================
    public boolean isClickedToMove() {
        return isClickedToMove;
    }

    public Piece getCheckingP() {
        return checkingP;
    }

    public int getDisplayCol(int col) {
        return (isMultiplayer && playerColor == BLACK) ? 7 - col : col;
    }

    public int getDisplayRow(int row) {
        return (isMultiplayer && playerColor == BLACK) ? 7 - row : row;
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

    public AudioManager getAudioManager() {
        return audioManager;
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
        if (netManager != null) {
            netManager.closeConnection();
            netManager = null;
        }
        isMultiplayer = false;
        isServer = false;
        opponentProfile = null;
        menuPanel.resetMenu();
        audioManager.playBGM(MENU_BGM);
        showPanel(menuPanel);
        window.revalidate();
        window.repaint();
    }

    public void makeAiMove(String uciMove) {
        if (uciMove == null || uciMove.length() < 4) {
            isAiThinking = false;
            return;
        }

        int startCol = uciMove.charAt(0) - 'a';
        int startRow = 8 - Character.getNumericValue(uciMove.charAt(1));
        int targetCol = uciMove.charAt(2) - 'a';
        int targetRow = 8 - Character.getNumericValue(uciMove.charAt(3));

        activeP = getPieceAt(startCol, startRow);

        if (activeP != null) {
            boolean isCapture = (activeP.gettingHitP(targetCol, targetRow) != null);
            simulateClickToMove(targetCol, targetRow);

            if (uciMove.length() == 5) {
                char promo = uciMove.charAt(4);
                int type = (promo == 'r') ? 1 : (promo == 'n') ? 2 : (promo == 'b') ? 3 : 0;
                replacePawnAndFinishNetwork(type);
            } else {
                activeP.finishMove();
            }

            playMoveSound(isCapture, castlingP != null);

            // GIẢI PHÓNG TRẠNG THÁI TRƯỚC KHI ĐỔI LƯỢT
            isAiThinking = false;

            // Hàm này sẽ tự gọi finalizeTurn() bên trong nó
            checkGameEndConditions();

            // TUYỆT ĐỐI KHÔNG GỌI finalizeTurn() Ở ĐÂY NỮA
        } else {
            isAiThinking = false;
            System.err.println("AI Error: Start square empty " + uciMove);
        }
    }

    public Piece getPieceAt(int col, int row) {
        return simPieces.stream()
                .filter(p -> p.col == col && p.row == row)
                .findFirst()
                .orElse(null);
    }

    public String getFEN() {
        StringBuilder fen = new StringBuilder();

        // 1. Piece placement: Duyệt từ hàng 0 (tương ứng hàng 8 của bàn cờ) đến hàng 7
        for (int r = 0; r < 8; r++) {
            int emptySquares = 0;
            for (int c = 0; c < 8; c++) {
                Piece p = getPieceAt(c, r); // Gọi hàm tìm quân cờ tại tọa độ (đã viết bằng Stream)

                if (p == null) {
                    emptySquares++;
                } else {
                    if (emptySquares > 0) {
                        fen.append(emptySquares);
                        emptySquares = 0;
                    }
                    fen.append(p.getFENChar()); // Gọi hàm lấy ký hiệu quân cờ
                }
            }
            if (emptySquares > 0) fen.append(emptySquares);
            if (r < 7) fen.append("/"); // Ngăn cách giữa các hàng
        }

        // 2. Active color: Lượt đi hiện tại ('w' hoặc 'b')
        fen.append(currentColor == WHITE ? " w " : " b ");

        // 3. Castling availability: Quyền nhập thành (Tạm thời để KQkq nếu còn đủ Xe/Vua)
        // Lưu ý: Để chuyên nghiệp hơn, bạn nên kiểm tra biến 'moved' của Vua và Xe
        fen.append("KQkq ");

        // 4. En passant target square: Ô có thể bắt quân qua đường (Tạm để '-' nếu chưa làm logic này)
        fen.append("- ");

        // 5. Halfmove clock & Fullmove number: Các chỉ số phụ cho luật hòa 50 nước
        fen.append("0 1");

        return fen.toString();
    }

    // =========================================================
    // NHÓM 3: MULTIPLAYER & SYNC (SỬA LỖI TẠI ĐÂY)
    // =========================================================
    public void setupMultiplayer(boolean host, int selectedColor, String ip) {
        this.isMultiplayer = true;
        this.isServer = host;
        this.playerColor = selectedColor;
        this.isTimeRunning = false;
        this.netManager = new NetworkManager(this);
        if (host) netManager.hostGame(5555);
        else netManager.joinGame(ip, 5555);
    }

    public void hostPressStart() {
        // HÀM NÀY CHỈ ĐƯỢC GỌI KHI HOST CLICK NÚT "START GAME" TRÊN PC
        if (isServer && isMultiplayer && netManager != null) {
            // Gửi lệnh -2 để máy Android (Joiner) cũng vào game
            netManager.sendMove(new MovePacket(-2, -2, -2, -2, -1));
            // 2. Gọi hiệu ứng trên chính máy Host PC
            JPanel current = (JPanel) window.getContentPane().getComponent(0);
            if (current instanceof LobbyPanel) {
                ((LobbyPanel) current).startSyncDelay();
            }
        }
    }

    public void setMyProfile(String n, int c) {
        this.myName = n;
        this.playerColor = c;
    }

    public void onOpponentConnected() {
        // 1. Khi có người kết nối, Host gửi cấu hình của mình đi trước
        if (isServer) {
            netManager.sendConfig(new GameConfigPacket(this.playerColor, this.myName));
        }

        // 2. Chuyển sang màn hình Lobby và ĐỢI, chưa được startNewGame()
        SwingUtilities.invokeLater(() -> {
            LobbyPanel lp = new LobbyPanel(this, myName, playerColor);
            showPanel(lp);
        });
    }

    // Sửa lại hàm này trong GameController.java (PC)
    public void onConfigReceived(GameConfigPacket p) {
        // 1. Tạo profile cục bộ ngay lập tức để tránh tranh chấp dữ liệu
        final PlayerProfile profile = new PlayerProfile(p.playerName, p.hostColor, "");
        this.opponentProfile = profile;

        if (!isServer) {
            this.playerColor = (p.hostColor == WHITE) ? BLACK : WHITE;
            netManager.sendConfig(new GameConfigPacket(this.playerColor, this.myName));

            SwingUtilities.invokeLater(() -> {
                LobbyPanel lp = new LobbyPanel(this, myName, playerColor);
                lp.setOpponent(profile); // Dùng biến profile cục bộ ở trên
                showPanel(lp);
            });
        } else {
            SwingUtilities.invokeLater(() -> {
                JPanel current = (JPanel) window.getContentPane().getComponent(0);
                if (current instanceof LobbyPanel) {
                    ((LobbyPanel) current).setOpponent(profile);
                }
            });
        }
    }

    public void receiveNetworkMove(MovePacket packet) {
        // 1. Nhận mã -2 (Lệnh bắt đầu game): CHỈ JOINER MỚI ĐƯỢC CHẤP NHẬN
        if (packet.oldCol == -2) {
            if (!isServer) { // Quan trọng: Nếu mình là Host thì lờ đi, tránh bị echo packet
                startNewGame();
            }
            return;
        }

        // 2. Nhận mã -3 (Tín hiệu đồng bộ Lobby)
        if (packet.oldCol == -3) {
            SwingUtilities.invokeLater(() -> {
                JPanel current = (JPanel) window.getContentPane().getComponent(0);
                if (current instanceof LobbyPanel) ((LobbyPanel) current).startSyncDelay();
            });
            return;
        }

        // 3. Nhận mã -1 (Yêu cầu chơi lại)
        if (packet.oldCol == -1) {
            handleRematchReceived();
            return;
        }

        // Logic xử lý nước đi thông thường (Không đổi)
        for (Piece p : simPieces) {
            if (p.col == packet.oldCol && p.row == packet.oldRow) {
                activeP = p;
                castlingP = null;
                activeP.canMove(packet.newCol, packet.newRow);
                simulateClickToMove(packet.newCol, packet.newRow);
                if (packet.promotionType != -1) replacePawnAndFinishNetwork(packet.promotionType);
                else activeP.finishMove();
                finalizeTurn();
                playMoveSound(activeP.hittingP != null, castlingP != null);
                checkGameEndConditions();
                break;
            }
        }
    }

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
            if (isServer) {
                // Nếu tôi là Host, tôi bấm chơi lại thì tôi phát lệnh Start cho cả 2
                hostPressStart();
            } else {
                // Nếu tôi là Client, tôi đồng ý thì tôi báo cho Host để Host bấm Start
                netManager.sendMove(new MovePacket(-1, -1, -1, -1, -1));
                JOptionPane.showMessageDialog(window, "Đã gửi xác nhận cho Host. Đợi Host bắt đầu...");
            }
        } else {
            exitToMenu();
        }
    }

    // =========================================================
    // NHÓM 4: LUẬT CHƠI (Giữ nguyên toàn bộ)
    // =========================================================
    public void finalizeTurn() {
        copyPieces(simPieces, pieces);
        pieces.forEach(Piece::updatePosition);
        isClickedToMove = false;
        activeP = null;
        validMoves.clear();
        castlingP = null;
        promotion = false;
        currentColor = (currentColor == WHITE) ? BLACK : WHITE;
        // Reset trạng thái vọt 2 ô của quân Tốt
        pieces.stream()
                .filter(p -> p.color == currentColor)
                .forEach(p -> p.twoStepped = false);
        resetTime();

        // KÍCH HOẠT AI CHẾ ĐỘ OFFLINE
        if (!isMultiplayer && currentColor == BLACK && !gameOver && !isAiThinking) {
            isAiThinking = true; // Khóa lại, không cho phép kích hoạt thêm luồng AI nào khác

            new Thread(() -> {
                try {
                    String fen = getFEN(); // Lấy FEN chuẩn
                    // Yêu cầu AI tính toán nước đi tốt nhất
                    String bestMove = sfClient.getBestMove(fen, 1000);

                    if (bestMove != null) {
                        SwingUtilities.invokeLater(() -> makeAiMove(bestMove));
                    }
                } finally {
                    // Đảm bảo dù có lỗi hay không, cờ sẽ được hạ xuống ở makeAiMove hoặc tại đây
                }
            }).start();
        }
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

    public boolean isCheckMate() {
        if (!isKingInCheck()) return false;
        for (Piece p : simPieces)
            if (p.color == currentColor)
                for (int r = 0; r < 8; r++)
                    for (int c = 0; c < 8; c++)
                        if (p.canMove(c, r) && simulateMoveAndKingSafe(p, c, r)) return false;
        return true;
    }

    public boolean isInsufficientMaterial() {
        boolean hasMajorPieces = simPieces.stream()
                .anyMatch(p -> p.type == Type.PAWN || p.type == Type.ROOK || p.type == Type.QUEEN);

        return !hasMajorPieces && simPieces.size() <= 3;
    }

    public boolean canPromote() {
        if (activeP == null || activeP.type != Type.PAWN) return false;
        return (activeP.color == WHITE && activeP.row == 0) || (activeP.color == BLACK && activeP.row == 7);
    }

    private void checkGameEndConditions() {
        if (isKingInCheck()) {
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

    private boolean isStaleMate() {
        for (Piece p : simPieces)
            if (p.color == currentColor)
                for (int r = 0; r < 8; r++)
                    for (int c = 0; c < 8; c++)
                        if (p.canMove(c, r) && simulateMoveAndKingSafe(p, c, r)) return false;
        return true;
    }

    // =========================================================
    // NHÓM 5: SAVE / LOAD (Giữ nguyên toàn bộ)
    // =========================================================
    public void saveGame(int s, BufferedImage img) {
        if (isMultiplayer) return;
        try {
            File dir = new File(System.getProperty("user.dir") + File.separator + "saves");
            if (!dir.exists()) dir.mkdirs();
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(dir, "save_" + s + ".dat")));
            oos.writeObject(new SaveData(new ArrayList<>(this.pieces), currentColor, timeLeft));
            oos.close();
            if (img != null) ImageIO.write(img, "png", new File(dir, "thumb_" + s + ".png"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadGame(int s) {
        try {
            File f = new File(System.getProperty("user.dir") + File.separator + "saves", "save_" + s + ".dat");
            if (!f.exists()) return;
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
            return ImageIO.read(new File(System.getProperty("user.dir") + File.separator + "saves", "thumb_" + s + ".png"));
        } catch (Exception e) {
            return null;
        }
    }

    public String getSlotMetadata(int s) {
        File f = new File(System.getProperty("user.dir") + File.separator + "saves", "save_" + s + ".dat");
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

    // =========================================================
    // NHÓM 6: THỰC THI & CẬP NHẬT (Giữ nguyên toàn bộ)
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
        if (GameState.currentState != State.PLAYING || gameOver) return;
        if (toastAlpha > 0) toastAlpha -= 0.016f;
        isKingInCheck();
        if (isInsufficientMaterial()) {
            triggerEndGame(true, null);
            return;
        }
        long now = System.currentTimeMillis();
        if (isTimeRunning && now - lastSecond >= 1000) {
            lastSecond = now;
            timeLeft--;
            if (timeLeft <= 0) handleTimeOut();
        }
        if (promotion) promoting();
        else handleMouseInput();
    }

    private void handleMouseInput() {
        if (isMultiplayer && currentColor != playerColor) {
            if (mouse.released) {
                toastAlpha = 1.0f;
                mouse.released = false;
            }
            return;
        }
        updateCursorState();
        if (!mouse.released) return;
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
        } else processMove(col, row);
        mouse.released = false;
    }

    private void processMove(int col, int row) {
        boolean valid = false;
        for (int[] mv : validMoves)
            if (mv[0] == col && mv[1] == row) {
                valid = true;
                break;
            }
        if (valid) {
            int oc = activeP.col, or = activeP.row;
            simulateClickToMove(col, row);
            if (canPromote()) {
                setPromoPieces();
                promotion = true;
            } else {
                activeP.finishMove();
                copyPieces(simPieces, pieces);
                if (isMultiplayer) netManager.sendMove(new MovePacket(oc, or, col, row, -1));
                playMoveSound(activeP.hittingP != null, castlingP != null);
                checkGameEndConditions();
            }
        } else cancelOrSwitchSelection(col, row);
    }

    public void calculateValidMoves(Piece p) {
        validMoves.clear();
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (p.canMove(c, r) && simulateMoveAndKingSafe(p, c, r))
                    validMoves.add(new int[]{c, r, (p.gettingHitP(c, r) != null ? 1 : 0)});
    }

    private boolean simulateMoveAndKingSafe(Piece p, int tc, int tr) {
        int oR = p.row, oC = p.col;
        Piece cap = p.gettingHitP(tc, tr);
        if (p.type == Type.KING && Math.abs(tc - oC) == 2) {
            if (opponentsCanCaptureKing()) return false;
            int step = (tc > oC) ? 1 : -1;
            p.col = oC + step;
            if (opponentsCanCaptureKing()) {
                p.col = oC;
                return false;
            }
            p.col = oC;
        }
        if (cap != null) simPieces.remove(cap);
        p.col = tc;
        p.row = tr;
        boolean safe = !opponentsCanCaptureKing();
        p.col = oC;
        p.row = oR;
        if (cap != null) simPieces.add(cap);
        return safe;
    }

    private boolean opponentsCanCaptureKing() {
        Piece king = getKing(false);
        if (king == null) return false;
        return simPieces.stream()
                .anyMatch(p -> p.color != king.color && p.canMove(king.col, king.row));
    }

    public Piece getKing(boolean opp) {
        int c = opp ? (currentColor == WHITE ? BLACK : WHITE) : currentColor;
        return simPieces.stream()
                .filter(p -> p.type == Type.KING && p.color == c)
                .findFirst()
                .orElse(null);
    }

    public void simulateClickToMove(int tc, int tr) {
        copyPieces(pieces, simPieces);
        activeP.canMove(tc, tr);
        if (castlingP != null) {
            if (castlingP.col == activeP.col + 3) castlingP.col = activeP.col + 1;
            else if (castlingP.col == activeP.col - 4) castlingP.col = activeP.col - 1;
            castlingP.moved = true;
            castlingP.preCol = castlingP.col;
            castlingP.preRow = castlingP.row;
            castlingP.updatePosition();
        }
        if (activeP.hittingP != null) simPieces.remove(activeP.hittingP);
        activeP.col = tc;
        activeP.row = tr;
        activeP.updatePosition();
        copyPieces(simPieces, pieces);
    }

    public void setPromoPieces() {
        promoPieces.clear();
        int pColor = activeP.color;
        promoPieces.add(new Queen(pColor, 0, 0));
        promoPieces.add(new Rook(pColor, 0, 0));
        promoPieces.add(new Bishop(pColor, 0, 0));
        promoPieces.add(new Knight(pColor, 0, 0));
        for (int i = 0; i < promoPieces.size(); i++) {
            Piece p = promoPieces.get(i);
            p.image = reloadPieceImage(p);
            p.col = activeP.col;
            p.row = (pColor == WHITE) ? i : 7 - i;
            p.updatePosition();
        }
    }

    public void promoting() {
        if (mouse.released) {
            int col = mouse.x / Board.SQUARE_SIZE, row = mouse.y / Board.SQUARE_SIZE;
            if (isMultiplayer && playerColor == BLACK) {
                col = 7 - col;
                row = 7 - row;
            }
            for (Piece p : promoPieces)
                if (p.col == col && p.row == row) {
                    replacePawnAndFinish(p);
                    break;
                }
            mouse.released = false;
        }
    }

    public void replacePawnAndFinish(Piece p) {
        int tC = activeP.col, tR = activeP.row;
        p.col = tC;
        p.row = tR;
        p.updatePosition();
        p.image = reloadPieceImage(p);
        simPieces.add(p);
        simPieces.remove(activeP);
        copyPieces(simPieces, pieces);
        int type = (p instanceof Rook) ? 1 : (p instanceof Knight) ? 2 : (p instanceof Bishop) ? 3 : 0;
        if (isMultiplayer) netManager.sendMove(new MovePacket(activeP.preCol, activeP.preRow, p.col, p.row, type));
        promotion = false;
        checkGameEndConditions();
    }

    public void replacePawnAndFinishNetwork(int type) {
        Piece newP;
        int color = activeP.color, r = activeP.row, c = activeP.col;
        switch (type) {
            case 1:
                newP = new Rook(color, r, c);
                break;
            case 2:
                newP = new Knight(color, r, c);
                break;
            case 3:
                newP = new Bishop(color, r, c);
                break;
            default:
                newP = new Queen(color, r, c);
                break;
        }
        newP.image = reloadPieceImage(newP);
        newP.updatePosition();
        simPieces.add(newP);
        simPieces.remove(activeP);
        copyPieces(simPieces, pieces);
    }

    private void triggerEndGame(boolean d, Integer w) {
        gameOver = true;
        isTimeRunning = false;
        isDraw = d;
        GameState.currentState = State.GAME_OVER;
        if (d) audioManager.playSFX("/audio/sfx/draw.wav");
        else audioManager.playSFX(w == WHITE ? "/audio/sfx/white_win.wav" : "/audio/sfx/black_win.wav");
    }

    private void handleTimeOut() {
        if (promotion) {
            if (!promoPieces.isEmpty()) replacePawnAndFinish(promoPieces.get(0));
            else finalizeTurn();
        } else if (isKingInCheck()) triggerEndGame(false, (currentColor == WHITE ? BLACK : WHITE));
        else finalizeTurn();
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

    private void playMoveSound(boolean cap, boolean castled) {
        if (castled) audioManager.playSFX("/audio/sfx/castle.wav");
        else if (cap) audioManager.playSFX("/audio/sfx/capture.wav");
        else audioManager.playSFX("/audio/sfx/move.wav");
        Timer t = new Timer(200, e -> {
            if (isKingInCheck() && !gameOver) audioManager.playSFX("/audio/sfx/check.wav");
        });
        t.setRepeats(false);
        t.start();
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
        validMoves.clear();
        for (Piece p : simPieces)
            if (p.color == currentColor && p.col == c && p.row == r) {
                activeP = p;
                calculateValidMoves(activeP);
                isClickedToMove = true;
                break;
            }

        simPieces.stream()
                .filter(p -> p.color == currentColor && p.col == c && p.row == r)
                .findFirst()
                .ifPresent(p -> {
                    activeP = p;
                    calculateValidMoves(activeP);
                    isClickedToMove = true;
                });
    }

    private BufferedImage reloadPieceImage(Piece p) {
        String pr = (p.color == WHITE) ? "w" : "b";
        String n = p.type.toString().substring(0, 1).toUpperCase() + p.type.toString().substring(1).toLowerCase();
        return p.getImage("/piece/" + pr + n);
    }

    private void setPieces() {
        pieces.clear();
        ChessSetupUtility.setupStandardGame(pieces);
        pieces.forEach(p -> {
            p.image = reloadPieceImage(p);
            p.updatePosition();
        });
    }

    public void copyPieces(CopyOnWriteArrayList<Piece> s, CopyOnWriteArrayList<Piece> t) {
        t.clear();
        t.addAll(s);
    }

    private void showPanel(JPanel p) {
        window.showPanel(p);
    }

    public void resetTime() {
        timeLeft = 20;
        lastSecond = System.currentTimeMillis();
    }

    public void handleMouseRelease(int x, int y) {
        mouse.released = true;
        mouse.x = x;
        mouse.y = y;
    }
}