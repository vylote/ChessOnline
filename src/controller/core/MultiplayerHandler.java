package controller.core;

import model.*;
import javax.swing.JOptionPane;

public class MultiplayerHandler {
    private final GameController gc;
    public MultiplayerHandler(GameController gc) { this.gc = gc; }

    public void handlePacket(MovePacket p) {
        if (p.oldCol == -4) { gc.finalizeTurn(); return; }
        if (p.oldCol == -2) { if (!gc.isServer) gc.startNewGame(); return; }
        if (p.oldCol == -1) {
            int res = JOptionPane.showConfirmDialog(gc.window, "Chơi lại?", "Rematch", 0);
            if (res == 0 && gc.isServer) gc.hostPressStart();
            return;
        }
        gc.getSimPieces().stream().filter(pc -> pc.col == p.oldCol && pc.row == p.oldRow)
                .findFirst().ifPresent(pc -> { gc.setActiveP(pc); gc.executeMove(p.newCol, p.newRow, true); });
    }

    public void handleConfig(GameConfigPacket p) {
        gc.setOpponentProfile(new PlayerProfile(p.playerName, p.hostColor, ""));
        if (!gc.isServer) {
            gc.playerColor = (p.hostColor == 0) ? 1 : 0;
            gc.netManager.sendConfig(new GameConfigPacket(gc.playerColor, gc.getMyName()));
            gc.window.showPanel(new view.LobbyPanel(gc, gc.getMyName(), gc.playerColor));
        }
    }
}