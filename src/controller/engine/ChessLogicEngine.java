package controller.engine;

import controller.core.GameController;
import model.*;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChessLogicEngine {
    public void calculateValidMoves(Piece p, CopyOnWriteArrayList<Piece> simPieces, CopyOnWriteArrayList<int[]> validMoves, int color) {
        validMoves.clear();
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                if (p.canMove(c, r) && simulateMoveAndKingSafe(p, c, r, simPieces, color)) {
                    int type = (p.gettingHitP(c, r) != null) ? 1 : 0;
                    validMoves.add(new int[]{c, r, type});
                }
            }
    }

    public boolean isKingInCheck(CopyOnWriteArrayList<Piece> simPieces, int color) {
        Piece k = simPieces.stream().filter(p -> p.type == Type.KING && p.color == color).findFirst().orElse(null);
        if (k == null) return false;
        return simPieces.stream().anyMatch(p -> p.color != color && p.canMove(k.col, k.row));
    }

    private boolean hasNoLegalMoves(CopyOnWriteArrayList<Piece> simPieces, int color) {
        for (Piece p : simPieces) {
            if (p.color == color) {
                for (int r = 0; r < 8; r++) {
                    for (int c = 0; c < 8; c++) {
                        // Nếu CHỈ CẦN CÓ 1 nước đi giúp Vua an toàn, thì KHÔNG phải Checkmate
                        if (p.canMove(c, r) && simulateMoveAndKingSafe(p, c, r, simPieces, color)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public boolean isCheckMate(CopyOnWriteArrayList<Piece> simPieces, int color) {
        return isKingInCheck(simPieces, color) && hasNoLegalMoves(simPieces, color);
    }

    public boolean isStaleMate(CopyOnWriteArrayList<Piece> simPieces, int color) {
        return !isKingInCheck(simPieces, color) && hasNoLegalMoves(simPieces, color);
    }

    public boolean simulateMoveAndKingSafe(Piece p, int tc, int tr, CopyOnWriteArrayList<Piece> simPieces, int color) {
        // ÉP các hàm canMove() của quân cờ nhìn vào danh sách giả lập này
        CopyOnWriteArrayList<Piece> backup = GameController.simPieces;
        GameController.simPieces = simPieces;

        int oc = p.col, or = p.row;
        Piece hit = p.gettingHitP(tc, tr);
        if (hit != null) simPieces.remove(hit);

        p.col = tc;
        p.row = tr;

        // Kiểm tra Vua an toàn trong danh sách simPieces
        boolean safe = !isKingInCheck(simPieces, color);

        // Hoàn trả
        p.col = oc;
        p.row = or;
        if (hit != null) simPieces.add(hit);

        // TRẢ LẠI danh sách thật cho Controller
        GameController.simPieces = backup;
        return safe;
    }
}