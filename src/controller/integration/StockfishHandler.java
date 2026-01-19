package controller.integration;

import controller.core.GameController;
import utility.StockfishClient;
import javax.swing.SwingUtilities;

public class StockfishHandler {
    private final StockfishClient client;
    private final GameController gc;
    private final String enginePath = "engines/stockfish.exe";

    public StockfishHandler(GameController gc) {
        this.gc = gc;
        this.client = new StockfishClient();
        restartEngine();
    }

    private void restartEngine() {
        client.startEngine(enginePath);
    }

    public void startThinking(String fen) {
        new Thread(() -> {
            String move = null;
            try {
                // Đảm bảo Engine còn sống trước khi gửi FEN
                move = client.getBestMove(fen, 500);
            } catch (Exception e) {
                System.err.println("Lỗi Engine Stockfish: " + e.getMessage());
                restartEngine(); // Tự khởi động lại nếu tiến trình chết
            } finally {
                final String finalMove = move;
                SwingUtilities.invokeLater(() -> {
                    // Luôn gọi makeAiMove dù kết quả là null để Controller xử lý kẹt lượt
                    gc.makeAiMove(finalMove);
                });
            }
        }).start();
    }
}