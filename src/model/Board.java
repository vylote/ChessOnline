package model;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class Board {
    public static final int MAX_COL = 8;
    public static final int MAX_ROW = 8;
    public static final int SQUARE_SIZE = 75; // 75 * 8 = 600
    public static final int BOARD_WIDTH = MAX_COL * SQUARE_SIZE;

    private BufferedImage boardImage;

    public Board() {
        try {
            boardImage = ImageIO.read(new File("res/board/board.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void draw(Graphics2D g2) {
        if (boardImage != null) {
            g2.drawImage(boardImage, 0, 0, BOARD_WIDTH, BOARD_WIDTH, null);
        }
    }
}