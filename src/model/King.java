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
            // Di chuyển 1 ô
            if (Math.abs(targetRow - preRow) + Math.abs(targetCol - preCol) == 1 ||
                    Math.abs(targetRow - preRow) * Math.abs(targetCol - preCol) == 1) {
                if (isValidSquare(targetCol, targetRow)) return true;
            }

            // NHẬP THÀNH
            if (!moved) {
                // Nhập thành phải (Cột 7)
                if (targetRow == preRow && targetCol == preCol + 2 && !pieceIsOnStraightline(targetCol, targetRow)) {
                    for (Piece piece : GameController.simPieces) {
                        if (piece.type == Type.ROOK && piece.color == this.color &&
                                piece.row == preRow && piece.col == preCol + 3 && !piece.moved) {
                            GameController.castlingP = piece;
                            return true;
                        }
                    }
                }
                // Nhập thành trái (Cột 0)
                if (targetRow == preRow && targetCol == preCol - 2 && !pieceIsOnStraightline(targetCol, targetRow)) {
                    Piece bSquarePiece = null; // Ô cột 1 (Knight square)
                    Piece rookPiece = null;    // Ô cột 0 (Rook square)

                    for (Piece piece : GameController.simPieces) {
                        if (piece.row == preRow) {
                            if (piece.col == preCol - 3) bSquarePiece = piece;
                            if (piece.col == preCol - 4) rookPiece = piece;
                        }
                    }
                    // FIX: Phải kiểm tra ô bSquarePiece (cột 1) có trống không (null)
                    if (bSquarePiece == null && rookPiece != null &&
                            rookPiece.type == Type.ROOK && !rookPiece.moved) {
                        GameController.castlingP = rookPiece;
                        return true;
                    }
                }
            }
        }
        return false;
    }
}