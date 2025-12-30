package model;

import java.io.Serializable;

public class PlayerProfile implements Serializable {
    public String name;
    public int color; // 0 cho Trắng, 1 cho Đen
    public String ip; // Dùng để kết nối TCP sau khi tìm thấy nhau

    public PlayerProfile(String name, int color, String ip) {
        this.name = name;
        this.color = color;
        this.ip = ip;
    }
}
