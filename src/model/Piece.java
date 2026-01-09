package model;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serializable;

import javax.imageio.ImageIO;

import controller.GameController;

public abstract class Piece implements Serializable {
    private static final long serialVersionUID = 1L;

    public Type type;
    public transient BufferedImage image;
    public int x, y;
    public int row, col, preRow, preCol;
    public int color;
    public Piece hittingP;
    public boolean moved, twoStepped;

    public Piece(int color, int row, int col) {
        this.color = color;
        this.row = row;
        this.col = col;
        x = getX(col);
        y = getY(row);
        preRow = row;
        preCol = col;
    }

    public void draw(Graphics2D g2) {
        g2.drawImage(image, x, y, Board.SQUARE_SIZE, Board.SQUARE_SIZE, null);
    }

    public BufferedImage getImage(String path) {

        BufferedImage image = null;

        try {
            image = ImageIO.read(getClass().getResourceAsStream(path+".png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return image;
    }

    public int getX(int col) { return col*Board.SQUARE_SIZE; }
    public int getY(int row) { return row*Board.SQUARE_SIZE; }

    public void updatePosition() {
        this.x = col * Board.SQUARE_SIZE;
        this.y = row * Board.SQUARE_SIZE;
    }

    public void finishMove() {
        updatePosition(); // Cập nhật x, y ngay lập tức cho Render
        if (type == Type.PAWN && Math.abs(preRow - row) == 2) {
            twoStepped = true;
        }
        preCol = col;
        preRow = row;
        moved = true;
    }

    public boolean canMove(int targetCol, int targetRow) {
        return false;
    }

    public boolean isWithinBoard(int targetCol, int targetRow) {
        return targetCol >= 0 && targetCol <= 7 && targetRow >= 0 && targetRow <= 7;
    }

    // return to a piece was hit by "this"
    public Piece gettingHitP(int targetCol, int targetRow) {
        return GameController.simPieces.stream()
                .filter(p -> p.col == targetCol && p.row == targetRow && p != this)
                .findFirst()
                .orElse(null);
    }

    public boolean isValidSquare(int targetCol, int targetRow) {
        hittingP = gettingHitP(targetCol, targetRow);
        if (hittingP == null) {
            return true;
        } else {
            if (hittingP.color != this.color) {
                return true;
            } else {
                hittingP = null;
            }
        }
        return false;
    }

    public boolean isSameSquare(int targetCol, int targetRow) {
        return targetCol == preCol && targetRow == preRow;
    }

    public boolean pieceIsOnStraightline(int targetCol, int targetRow) {
        //move to the left
        if (preRow == targetRow && preCol > targetCol) {
            return GameController.simPieces.stream()
                    .anyMatch(p -> p.row == targetRow && p.col < preCol && p.col > targetCol);
        }
        //move to the right
        if (preRow == targetRow && preCol < targetCol) {
            return GameController.simPieces.stream()
                    .anyMatch(p -> p.row == targetRow && p.col > preCol && p.col < targetCol);
        }
        //move up
        if (preCol == targetCol && preRow > targetRow) {
            return GameController.simPieces.stream()
                    .anyMatch(p -> p.col == targetCol && p.row < preRow && p.row > targetRow);
        }
        //move down
        if (preCol == targetCol && preRow < targetRow) {
            return GameController.simPieces.stream()
                    .anyMatch(p -> p.col == targetCol && p.row > preRow && p.row < targetRow);
        }
        return false;
    }

    public boolean pieceIsOnDiagonalLine(int targetCol, int targetRow) {
        return GameController.simPieces.stream()
                .filter(p -> p != this) // 1. Loại bỏ chính quân cờ đang xét
                .filter(p -> Math.abs(p.col - preCol) == Math.abs(p.row - preRow)) // 2. Kiểm tra p có nằm trên đường chéo không
                .anyMatch(p -> {
                    // 3. Kiểm tra xem p có nằm TRONG phạm vi từ vị trí cũ đến vị trí đích không
                    boolean withinCol = (preCol < targetCol) ? (p.col > preCol && p.col < targetCol)
                            : (p.col < preCol && p.col > targetCol);
                    boolean withinRow = (preRow < targetRow) ? (p.row > preRow && p.row < targetRow)
                            : (p.row < preRow && p.row > targetRow);

                    if (withinCol && withinRow) {
                        hittingP = p; // Gán quân cờ cản đường để xử lý logic va chạm
                        return true;
                    }
                    return false;
                });
    }

    public char getFENChar() {
        char fenChar = switch (type) {
            case PAWN -> 'p';
            case ROOK -> 'r';
            case KNIGHT -> 'n';
            case BISHOP -> 'b';
            case QUEEN -> 'q';
            case KING -> 'k';
        };

        // Nếu là quân Trắng (WHITE = 0), chuyển thành chữ HOA
        return (color == GameController.WHITE) ? Character.toUpperCase(fenChar) : fenChar;
    }
}