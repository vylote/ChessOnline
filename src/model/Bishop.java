package model;

import controller.GameController;

public class Bishop extends Piece {

    public Bishop(int color, int row, int col) {
        super(color, row, col);

        type = Type.BISHOP;

        if (color == GameController.WHITE) {
            image = getImage("/piece/wBishop");
        } else {
            image = getImage("/piece/bBishop");
        }

    }

    public boolean canMove(int targetCol, int targetRow) {
        if (isWithinBoard(targetCol, targetRow) && !isSameSquare(targetCol, targetRow)) {
            if (Math.abs(targetRow - preRow) == Math.abs(targetCol - preCol)) {
                return isValidSquare(targetCol, targetRow) && !pieceIsOnDiagonalLine(targetCol, targetRow);
            }
        }
        return false;
    }
}