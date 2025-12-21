package model;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/*
 * import java.awt.Color; import java.awt.Graphics2D; import
 * java.awt.image.BufferedImage; import java.io.File; import
 * java.io.IOException;
 *
 * import javax.imageio.ImageIO;
 *
 * public class Board { final int MAX_COL = 8; final int MAX_ROW = 8; public
 * static final int SQUARE_SIZE = 100; public static final int HAFL_SQUARE_SIZE
 * = SQUARE_SIZE / 2;
 *
 * public void draw(Graphics2D g2) { int c = 1; for (int row = 0; row < MAX_ROW;
 * row++) { if (c == 1) c = 0; else c = 1; for (int col = 0; col < MAX_COL;
 * col++) { if (c == 0) { g2.setColor(new Color(250, 201, 159)); c = 1; } else {
 * g2.setColor(new Color(171, 85, 9)); c = 0; } g2.fillRect(col * SQUARE_SIZE,
 * row * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE); } } } }
 */


// load board image

public class Board {
    private BufferedImage boardImage;

    // constant
    public static final int MAX_COL = 8;
    public static final int MAX_ROW = 8;
    public static int SQUARE_SIZE = 100;
    public static final int HAFL_SQUARE_SIZE = SQUARE_SIZE / 2;

    public static final int BOARD_WIDTH = MAX_COL * SQUARE_SIZE;


    public Board() {
        try {
            boardImage = ImageIO.read(new File("res/board/board.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void draw(Graphics2D g2) {
        if (boardImage != null) {
            g2.drawImage(boardImage, 0, 0, MAX_COL * SQUARE_SIZE, MAX_ROW * SQUARE_SIZE, null);
        } else {
            System.out.println("Board image not loaded.");
        }
    }
}