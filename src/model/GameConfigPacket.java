package model;

import java.io.Serializable;

public class GameConfigPacket implements Serializable {
    private static final long serialVersionUID = 1L;
    public int hostColor;

    public GameConfigPacket(int hostColor) {
        this.hostColor = hostColor;
    }
}