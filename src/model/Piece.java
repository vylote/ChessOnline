package model;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serializable;
import javax.imageio.ImageIO;
import controller.core.GameController;

public abstract class Piece implements Serializable {
    private static final long serialVersionUID = 1L;

    public Type type;
    public transient BufferedImage image;
    public int x, y, row, col, preRow, preCol;
    public int color;
    public Piece hittingP;
    public boolean moved, twoStepped;

    public Piece(int color, int row, int col) {
        this.color = color;
        this.row = row;
        this.col = col;
        updatePosition();
        preRow = row;
        preCol = col;
    }

    public abstract boolean canMove(int targetCol, int targetRow);

    public void draw(Graphics2D g2) {
        g2.drawImage(image, x, y, Board.SQUARE_SIZE, Board.SQUARE_SIZE, null);
    }

    public BufferedImage getImage(String path) {
        try {
            return ImageIO.read(getClass().getResourceAsStream(path + ".png"));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void updatePosition() {
        this.x = col * Board.SQUARE_SIZE;
        this.y = row * Board.SQUARE_SIZE;
    }

    public void finishMove() {
        updatePosition();
        if (type == Type.PAWN && Math.abs(preRow - row) == 2) twoStepped = true;
        preCol = col;
        preRow = row;
        moved = true;
    }

    public boolean isWithinBoard(int targetCol, int targetRow) {
        return targetCol >= 0 && targetCol <= 7 && targetRow >= 0 && targetRow <= 7;
    }

    public Piece gettingHitP(int targetCol, int targetRow) {
        return GameController.simPieces.stream()
                .filter(p -> p.col == targetCol && p.row == targetRow && p != this)
                .findFirst().orElse(null);
    }

    public boolean isValidSquare(int targetCol, int targetRow) {
        hittingP = gettingHitP(targetCol, targetRow);
        if (hittingP == null) return true;

        // CHẶN TUYỆT ĐỐI: Không ăn quân cùng màu và KHÔNG ăn Vua
        if (hittingP.color != this.color && hittingP.type != Type.KING) {
            return true;
        }
        hittingP = null;
        return false;
    }

    public boolean isSameSquare(int targetCol, int targetRow) {
        return targetCol == preCol && targetRow == preRow;
    }

    public boolean pieceIsOnStraightline(int targetCol, int targetRow) {
        if (preRow == targetRow && preCol > targetCol) // Left
            return GameController.simPieces.stream().anyMatch(p -> p.row == targetRow && p.col < preCol && p.col > targetCol);
        if (preRow == targetRow && preCol < targetCol) // Right
            return GameController.simPieces.stream().anyMatch(p -> p.row == targetRow && p.col > preCol && p.col < targetCol);
        if (preCol == targetCol && preRow > targetRow) // Up
            return GameController.simPieces.stream().anyMatch(p -> p.col == targetCol && p.row < preRow && p.row > targetRow);
        if (preCol == targetCol && preRow < targetRow) // Down
            return GameController.simPieces.stream().anyMatch(p -> p.col == targetCol && p.row > preRow && p.row < targetRow);
        return false;
    }

    public boolean pieceIsOnDiagonalLine(int targetCol, int targetRow) {
        return GameController.simPieces.stream()
                .filter(p -> p != this && Math.abs(p.col - preCol) == Math.abs(p.row - preRow))
                .anyMatch(p -> {
                    boolean withinCol = (preCol < targetCol) ? (p.col > preCol && p.col < targetCol) : (p.col < preCol && p.col > targetCol);
                    boolean withinRow = (preRow < targetRow) ? (p.row > preRow && p.row < targetRow) : (p.row < preRow && p.row > targetRow);
                    return withinCol && withinRow;
                });
    }

    public char getFENChar() {
        char c = switch (type) {
            case PAWN -> 'p'; case ROOK -> 'r'; case KNIGHT -> 'n';
            case BISHOP -> 'b'; case QUEEN -> 'q'; case KING -> 'k';
        };
        return (color == GameController.WHITE) ? Character.toUpperCase(c) : c;
    }
}