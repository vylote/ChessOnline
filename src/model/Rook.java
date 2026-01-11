package model;

import controller.core.GameController;

public class Rook extends Piece {

    public Rook(int color, int row, int col) {
        super(color, row, col);

        type = Type.ROOK;

        if (color == GameController.WHITE) {
            image = getImage("/piece/wRook");
        } else {
            image = getImage("/piece/bRook");
        }

    }

    public boolean canMove(int targetCol, int targetRow) {
        if (isWithinBoard(targetCol, targetRow) && !isSameSquare(targetCol, targetRow)) {
            if (targetCol == preCol || targetRow == preRow) {
                return isValidSquare(targetCol, targetRow) && !pieceIsOnStraightline(targetCol, targetRow);
            }
        }
        return false;
    }
}