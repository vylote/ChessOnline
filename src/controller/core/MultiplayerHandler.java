package controller.core;

import model.GameConfigPacket;
import model.MovePacket;
import model.PlayerProfile;
import view.LobbyPanel;

import javax.swing.*;

public class MultiplayerHandler {
    private final GameController gc;

    public MultiplayerHandler(GameController gc) { this.gc = gc; }

    /** Xử lý nước đi hoặc lệnh từ mạng */
    public void processMove(MovePacket p) {
        if (p.oldCol == -4) { gc.finalizeTurn(); return; }
        if (p.oldCol == -2) { if (!gc.isServer) gc.startNewGame(); return; }
        if (p.oldCol == -1) { handleRematch(); return; }

        gc.getSimPieces().stream()
                .filter(pc -> pc.col == p.oldCol && pc.row == p.oldRow)
                .findFirst()
                .ifPresent(pc -> {
                    gc.setActiveP(pc);
                    gc.executeMove(p.newCol, p.newRow, true);
                });
    }

    /** Xử lý cấu hình phòng chơi */
    public void processConfig(GameConfigPacket p) {
        gc.setOpponentProfile(new PlayerProfile(p.playerName, p.hostColor, ""));
        if (!gc.isServer) {
            gc.playerColor = (p.hostColor == 0) ? 1 : 0;
            gc.netManager.sendConfig(new GameConfigPacket(gc.playerColor, gc.getMyName()));
            LobbyPanel lp = new LobbyPanel(gc, gc.getMyName(), gc.playerColor);
            lp.setOpponent(gc.getOpponentProfile());
            gc.window.showPanel(lp);
        }
    }

    private void handleRematch() {
        int res = JOptionPane.showConfirmDialog(gc.window, "Đối thủ muốn chơi lại?", "Rematch", JOptionPane.YES_NO_OPTION);
        if (res == JOptionPane.YES_OPTION && gc.isServer) gc.hostPressStart();
    }
}