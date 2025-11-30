package model;

import controller.GameController;

public class Knight extends Piece {

    public Knight(int color, int row, int col) {
        super(color, row, col);

        type = Type.KNIGHT;

        if (color == GameController.WHITE) {
            image = getImage("/piece/wKnight");
        } else {
            image = getImage("/piece/bKnight");
        }

    }

    public boolean canMove(int targetCol, int targetRow) {
        if (isWithinBoard(targetCol, targetRow) && !isSameSquare(targetCol, targetRow)) {
            if (Math.abs(targetRow - preRow) * Math.abs(targetCol - preCol) == 2) {
                return isValidSquare(targetCol, targetRow);
            }
        }
        return false;
    }
}