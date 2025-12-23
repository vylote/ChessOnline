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
            int turn = (color == GameController.WHITE) ? -1 : 1;
            hittingP = gettingHitP(targetCol, targetRow);

            // 1. Đi thẳng 1 bước
            if (targetCol == col && targetRow == row + turn && hittingP == null) {
                return true;
            }

            // 2. Đi thẳng 2 bước (Chỉ khi chưa moved)
            if (targetCol == col && targetRow == row + turn * 2 && hittingP == null && !moved) {
                if (gettingHitP(targetCol, row + turn) == null) {
                    return true;
                }
            }

            // 3. Ăn quân chéo thường
            if (Math.abs(targetCol - col) == 1 && targetRow == row + turn && hittingP != null && hittingP.color != this.color) {
                return true;
            }

            // 4. En Passant
            if (targetRow == row + turn && Math.abs(targetCol - col) == 1) {
                for (Piece piece : GameController.simPieces) {
                    if (piece.col == targetCol && piece.row == row && piece.twoStepped) {
                        hittingP = piece;
                        return true;
                    }
                }
            }
        }
        return false;
    }
}