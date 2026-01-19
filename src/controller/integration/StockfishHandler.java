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
                // Giảm movetime xuống 500ms để AI phản hồi nhanh hơn, tránh lag luồng
                move = client.getBestMove(fen, 500);
            } catch (Exception e) {
                System.err.println("Lỗi Stockfish: " + e.getMessage());
            } finally {
                final String finalMove = move;
                SwingUtilities.invokeLater(() -> {
                    if (finalMove != null) {
                        gc.makeAiMove(finalMove);
                    } else {
                        // NẾU AI LỖI: Buộc phải mở khóa để người chơi đi tiếp
                        gc.isAiThinking = false;
                        gc.validMoves.clear();
                    }
                });
            }
        }).start();
    }
}