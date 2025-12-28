package model;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class Board {
    public static final int MAX_COL = 8;
    public static final int MAX_ROW = 8;
    public static final int SQUARE_SIZE = 75;
    public static final int BOARD_WIDTH = MAX_COL * SQUARE_SIZE;

    private BufferedImage boardImage;
    private BufferedImage boardFlippedImage; // Ảnh đảo ngược bạn đã chuẩn bị

    public Board() {
        try {
            // Nạp ảnh thuận
            boardImage = ImageIO.read(getClass().getResourceAsStream("/board/board.png"));
            // Nạp ảnh đảo (Đảm bảo file này nằm đúng trong thư mục res/board/)
            boardFlippedImage = ImageIO.read(getClass().getResourceAsStream("/board/board_flipped.png"));
        } catch (Exception e) {
            System.err.println("Không thể nạp ảnh bàn cờ: " + e.getMessage());
        }
    }

    // Cập nhật hàm draw để nhận thêm trạng thái flipped
    public void draw(Graphics2D g2, boolean flipped) {
        BufferedImage imgToDraw = flipped ? boardFlippedImage : boardImage;

        // Nếu chẳng may không tìm thấy ảnh đảo, dùng ảnh thuận làm dự phòng
        if (imgToDraw == null) imgToDraw = boardImage;

        if (imgToDraw != null) {
            g2.drawImage(imgToDraw, 0, 0, BOARD_WIDTH, BOARD_WIDTH, null);
        }
    }
}