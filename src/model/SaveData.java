package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class SaveData implements Serializable {
    private static final long serialVersionUID = 1L;

    public ArrayList<Piece> pieces;
    public int currentColor;
    public int timeLeft;
    public String saveTime; // Lưu thời gian thực tế

    public SaveData(ArrayList<Piece> pieces, int currentColor, int timeLeft) {
        this.pieces = pieces;
        this.currentColor = currentColor;
        this.timeLeft = timeLeft;

        // Tự động lấy thời gian hiện tại khi khởi tạo (Format: 2025-12-23 15:30)
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.saveTime = dtf.format(LocalDateTime.now());
    }
}