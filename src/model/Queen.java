package model;

import controller.GameController;

public class Queen extends Piece {

    public Queen(int color, int row, int col) {
        super(color, row, col);

        type = Type.QUEEN;

        if (color == GameController.WHITE) {
            image = getImage("/piece/wQueen");
        } else {
            image = getImage("/piece/bQueen");
        }

    }

    public boolean canMove(int targetCol, int targetRow) {
        if (isWithinBoard(targetCol, targetRow) && !isSameSquare(targetCol, targetRow)) {
            if (Math.abs(targetRow - preRow) == Math.abs(targetCol - preCol)) {
                if (isValidSquare(targetCol, targetRow) && !pieceIsOnDiagonalLine(targetCol, targetRow)) {
                    return true;
                }
            }
            if (targetCol == preCol || targetRow == preRow) {
                return isValidSquare(targetCol, targetRow) && !pieceIsOnStraightline(targetCol, targetRow);
            }
        }
        return false;
    }

}