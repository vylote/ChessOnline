package model;

import controller.GameController;

public class Pawn extends Piece {

    public Pawn(int color, int row, int col) {
        super(color, row, col);

        type = Type.PAWN;

        if (color == GameController.WHITE) {
            image = getImage("/piece/wPawn");
        } else {
            image = getImage("/piece/bPawn");
        }
    }

    public boolean canMove(int targetCol, int targetRow) {

        if (isWithinBoard(targetCol, targetRow) && !isSameSquare(targetCol, targetRow)) {
            int turn;
            if (color == GameController.WHITE) {
                turn = -1;
            } else {
                turn = 1;
            }

            hittingP = gettingHitP(targetCol, targetRow);

            // 1 square movement
            if (targetCol == preCol && targetRow == preRow + turn && hittingP == null) {
                return true;
            }
            // 2 square movement
            if (targetCol == preCol && targetRow == preRow + turn * 2 && hittingP == null && !moved
                    && !pieceIsOnStraightline(targetCol, targetRow)) {
                return true;
            }
            // hitting diagonal
            if ((Math.abs(targetRow - preRow) == 1 && Math.abs(targetCol - preCol) == 1 && hittingP != null
                    && targetRow == preRow + turn && hittingP.color != this.color)) {
                return true;
            }
            // en passant
            if (targetRow == preRow + turn && Math.abs(targetCol - preCol) == 1) {
                for (Piece piece : GameController.simPieces) {
                    if (piece.col == targetCol && piece.row == preRow && piece.twoStepped) {
                        hittingP = piece;
                        return true;
                    }
                }
            }
        }
        return false;
    }
}