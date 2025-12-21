package model;

import java.io.Serializable;
import java.util.ArrayList;

public class SaveData implements Serializable {
    private static final long serialVersionUID = 1L;

    public ArrayList<Piece> pieces;
    public int currentColor;
    public int timeLeft;

    public SaveData(ArrayList<Piece> pieces, int currentColor, int timeLeft) {
        this.pieces = pieces;
        this.currentColor = currentColor;
        this.timeLeft = timeLeft;
    }
}
