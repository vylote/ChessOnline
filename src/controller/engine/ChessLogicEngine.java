package controller.engine;

import model.Piece;
import model.Type;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChessLogicEngine {

    /** Tính toán nước đi và trả về mảng 3 tham số để GamePanel tô màu */
    public void calculateValidMoves(Piece p, CopyOnWriteArrayList<Piece> simPieces, ArrayList<int[]> validMoves, int currentColor) {
        validMoves.clear();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (p.canMove(c, r) && simulateMoveAndKingSafe(p, c, r, simPieces, currentColor)) {
                    // type 0: ô trống (xanh), type 1: ô có quân địch (đỏ)
                    int type = (p.gettingHitP(c, r) != null) ? 1 : 0;
                    validMoves.add(new int[]{c, r, type});
                }
            }
        }
    }

    public boolean isKingInCheck(CopyOnWriteArrayList<Piece> simPieces, int targetColor) {
        Piece king = simPieces.stream().filter(pc -> pc.type == Type.KING && pc.color == targetColor).findFirst().orElse(null);
        if (king == null) return false;
        for (Piece p : simPieces) {
            if (p.color != targetColor && p.canMove(king.col, king.row)) return true;
        }
        return false;
    }

    public boolean isCheckMate(CopyOnWriteArrayList<Piece> simPieces, int color) {
        if (!isKingInCheck(simPieces, color)) return false;
        return hasNoLegalMoves(simPieces, color);
    }

    public boolean isStaleMate(CopyOnWriteArrayList<Piece> simPieces, int color) {
        if (isKingInCheck(simPieces, color)) return false;
        return hasNoLegalMoves(simPieces, color);
    }

    private boolean hasNoLegalMoves(CopyOnWriteArrayList<Piece> simPieces, int color) {
        for (Piece p : simPieces) {
            if (p.color == color) {
                for (int r = 0; r < 8; r++) {
                    for (int c = 0; c < 8; c++) {
                        if (p.canMove(c, r) && simulateMoveAndKingSafe(p, c, r, simPieces, color)) return false;
                    }
                }
            }
        }
        return true;
    }

    public boolean simulateMoveAndKingSafe(Piece p, int tc, int tr, CopyOnWriteArrayList<Piece> simPieces, int color) {
        int oc = p.col, or = p.row;
        Piece hit = p.gettingHitP(tc, tr);
        if (hit != null) simPieces.remove(hit);
        p.col = tc; p.row = tr;
        boolean safe = !isKingInCheck(simPieces, color);
        p.col = oc; p.row = or;
        if (hit != null) simPieces.add(hit);
        return safe;
    }
}