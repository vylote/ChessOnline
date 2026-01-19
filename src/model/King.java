package model;

import controller.core.GameController;

public class King extends Piece {
    public King(int color, int row, int col) {
        super(color, row, col);
        this.type = Type.KING;
        this.image = getImage(color == GameController.WHITE ? "/piece/wKing" : "/piece/bKing");
    }

    public boolean canMove(int targetCol, int targetRow) {
        if (!isWithinBoard(targetCol, targetRow) || isSameSquare(targetCol, targetRow)) return false;

        // Di chuyển 1 ô: Sử dụng col/row hiện tại thay vì preCol/preRow để hỗ trợ giả lập
        if (Math.abs(targetRow - row) <= 1 && Math.abs(targetCol - col) <= 1) {
            return isValidSquare(targetCol, targetRow);
        }

        // NHẬP THÀNH
        if (!moved && targetRow == row) {
            // Nhập thành phải (Kingside)
            if (targetCol == col + 2 && !pieceIsOnStraightline(col + 3, targetRow)) {
                Piece rook = GameController.simPieces.stream()
                        .filter(p -> p.type == Type.ROOK && p.color == this.color && p.col == col + 3 && !p.moved)
                        .findFirst().orElse(null);
                if (rook != null) {
                    GameController.castlingP = rook; // QUAN TRỌNG: Gán để Xe di chuyển theo
                    return true;
                }
            }
            // Nhập thành trái (Queenside)
            if (targetCol == col - 2 && !pieceIsOnStraightline(col - 4, targetRow)) {
                boolean bSquareEmpty = GameController.simPieces.stream().noneMatch(p -> p.row == row && p.col == col - 3);
                Piece rook = GameController.simPieces.stream()
                        .filter(p -> p.type == Type.ROOK && p.color == this.color && p.col == col - 4 && !p.moved)
                        .findFirst().orElse(null);
                if (bSquareEmpty && rook != null) {
                    GameController.castlingP = rook; // QUAN TRỌNG
                    return true;
                }
            }
        }
        return false;
    }
}