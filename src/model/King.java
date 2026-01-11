package model;

import controller.GameController;

public class King extends Piece {
    public King(int color, int row, int col) {
        super(color, row, col);
        this.type = Type.KING;
        this.image = getImage(color == GameController.WHITE ? "/piece/wKing" : "/piece/bKing");
    }

    public boolean canMove(int targetCol, int targetRow) {
        if (!isWithinBoard(targetCol, targetRow) || isSameSquare(targetCol, targetRow)) return false;

        // Di chuyển 1 ô thông thường
        if (Math.abs(targetRow - preRow) <= 1 && Math.abs(targetCol - preCol) <= 1) {
            return isValidSquare(targetCol, targetRow);
        }

        // NHẬP THÀNH
        if (!moved && targetRow == preRow) {
            // Nhập thành phải (Kingside)
            if (targetCol == preCol + 2 && !pieceIsOnStraightline(preCol + 3, targetRow)) {
                return GameController.simPieces.stream().anyMatch(p -> p.type == Type.ROOK &&
                        p.color == this.color && p.col == preCol + 3 && !p.moved);
            }
            // Nhập thành trái (Queenside)
            if (targetCol == preCol - 2 && !pieceIsOnStraightline(preCol - 4, targetRow)) {
                // Kiểm tra ô cột B (col 1) phải trống
                boolean bSquareEmpty = GameController.simPieces.stream().noneMatch(p -> p.row == preRow && p.col == preCol - 3);
                return bSquareEmpty && GameController.simPieces.stream().anyMatch(p -> p.type == Type.ROOK &&
                        p.color == this.color && p.col == preCol - 4 && !p.moved);
            }
        }
        return false;
    }
}