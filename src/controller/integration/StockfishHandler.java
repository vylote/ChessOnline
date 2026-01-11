package controller.integration;

import controller.core.GameController;
import utility.StockfishClient;

import javax.swing.*;

public class StockfishHandler {
    private final StockfishClient client;
    private final GameController gc;

    public StockfishHandler(GameController gc) {
        this.gc = gc;
        this.client = new StockfishClient();
        this.client.startEngine("engines/stockfish.exe");
    }

    public void startThinking(String fen) {
        new Thread(() -> {
            try {
                String move = client.getBestMove(fen, 1000);
                if (move != null) SwingUtilities.invokeLater(() -> gc.makeAiMove(move));
            } catch (Exception e) {
                // Tự động khởi động lại nếu Pipe bị đóng
                client.startEngine("engines/stockfish.exe");
            }
        }).start();
    }
}