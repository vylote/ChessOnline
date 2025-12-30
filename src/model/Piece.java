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
    // Trong Piece.java
    public void updatePosition() {
        // Chỉ cập nhật tọa độ đồ họa dựa trên col và row hiện tại
        x = getX(col);
        y = getY(row);

        // Xóa dòng moved = true; ở đây
    }

    // Thêm một hàm mới để gọi khi kết thúc một nước đi thực tế
    public void finishMove() {
        updatePosition();
        if (type == Type.PAWN && Math.abs(preRow - row) == 2) {
            twoStepped = true;
        }
        preCol = col;
        preRow = row;
        moved = true; // Chỉ đánh dấu đã di chuyển ở đây
    }

    public boolean canMove(int targetCol, int targetRow) {
        return false;
    }

    public boolean isWithinBoard(int targetCol, int targetRow) {
        return targetCol >= 0 && targetCol <= 7 && targetRow >= 0 && targetRow <= 7;
    }

    // return to a piece was hit by "this"
    public Piece gettingHitP(int targetCol, int targetRow) {
        for (Piece piece : GameController.simPieces) {
            if (piece.col == targetCol && piece.row == targetRow && piece != this) {
                return piece;
            }
        }
        return null;
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

//    public int getIndex() {
//        for (int index = 0; index < GameController.simPieces.size(); ++index) {
//            if (GameController.simPieces.get(index) == this) {
//                return index;
//            }
//        }
//        return 0;
//    }

    public boolean isSameSquare(int targetCol, int targetRow) {
        return targetCol == preCol && targetRow == preRow;
    }

    public boolean pieceIsOnStraightline(int targetCol, int targetRow) {
        //move to the left
        for (int c = preCol-1; c > targetCol; c--) {
            for (Piece piece : GameController.simPieces) {
                if (piece.row == targetRow && piece.col == c) {
                    hittingP = piece;
                    return true;
                }
            }
        }
        //move to the right
        for (int c = preCol+1; c < targetCol; c++) {
            for (Piece piece : GameController.simPieces) {
                if (piece.row == targetRow && piece.col == c) {
                    hittingP = piece;
                    return true;
                }
            }
        }
        //move up
        for (int r = preRow-1; r > targetRow; r--) {
            for (Piece piece : GameController.simPieces) {
                if (piece.col == targetCol && piece.row == r) {
                    hittingP = piece;
                    return true;
                }
            }
        }
        //move down
        for (int r = preRow+1; r < targetRow; r++) {
            for (Piece piece : GameController.simPieces) {
                if (piece.col == targetCol && piece.row == r) {
                    hittingP = piece;
                    return true;
                }
            }
        }
        return false;
    }

    public boolean pieceIsOnDiagonalLine(int targetCol, int targetRow) {
        int diff;
        if (targetRow < preRow) {
            //up left
            for (int c = preCol-1; c > targetCol; c--) {
                diff = Math.abs(c-preCol);
                for (Piece piece : GameController.simPieces) {
                    if (piece.row == preRow-diff && piece.col == c) {
                        hittingP = piece;
                        return true;
                    }
                }
            }
            //up right
            for (int c = preCol+1; c < targetCol; c++) {
                diff = Math.abs(c-preCol);
                for (Piece piece : GameController.simPieces) {
                    if (piece.row == preRow-diff && piece.col == c) {
                        hittingP = piece;
                        return true;
                    }
                }
            }

        }
        if (targetRow > preRow) {
            //down left
            for (int c = preCol-1; c > targetCol; c--) {
                diff = Math.abs(c-preCol);
                for (Piece piece : GameController.simPieces) {
                    if (piece.row == preRow+diff && piece.col == c) {
                        hittingP = piece;
                        return true;
                    }
                }
            }
            //down right
            for (int c = preCol+1; c < targetCol; c++) {
                diff = Math.abs(c-preCol);
                for (Piece piece : GameController.simPieces) {
                    if (piece.row == preRow+diff && piece.col == c) {
                        hittingP = piece;
                        return true;
                    }
                }
            }
        }
        return false;
    }
}