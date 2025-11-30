package model;

import controller.GameController;

public class King extends Piece {

    public King(int color, int row, int col) {
        super(color, row, col);

        type = Type.KING;

        if (color == GameController.WHITE) {
            image = getImage("/piece/wKing");
        } else {
            image = getImage("/piece/bKing");
        }
    }

    public boolean canMove(int targetCol, int targetRow) {
        if (isWithinBoard(targetCol, targetRow) && !isSameSquare(targetCol, targetRow)) {

            // movement
            if (Math.abs(targetRow-preRow) + Math.abs(targetCol-preCol) == 1 ||
                    Math.abs(targetRow-preRow) * Math.abs(targetCol-preCol) == 1) {
                if (isValidSquare(targetCol, targetRow)) {
                    return true;
                }
            }

            // castling
            if (!moved) {
                // right castling
                if (targetRow == preRow && targetCol == preCol+2 && !pieceIsOnStraightline(targetCol, targetRow)) {
                    for (Piece piece : GameController.simPieces) {
                        if (piece.col == preCol+3 && piece.row == preRow && !piece.moved) {
                            GameController.castlingP = piece;
                            return true;
                        }
                    }
                }

                // left castling
                if (targetRow == preRow && targetCol == preCol-2 && !pieceIsOnStraightline(targetCol, targetRow)) {
                    Piece[] p = new Piece[2];
                    for (Piece piece : GameController.simPieces) {
                        if (piece.col == preCol-3 && piece.row == preRow) {
                            p[0] = piece;
                        }
                        if (piece.col == preCol-4 && piece.row == preRow) {
                            p[1] = piece;
                        }
                        if (p[0] == null && p[1] != null && !p[1].moved) {
                            GameController.castlingP = p[1];
                            return true;
                        }
                    }
                }

            }
        }
        return false;
    }
}