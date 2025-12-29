package model;

import java.io.Serializable;

public class MovePacket implements Serializable {
    private static final long serialVersionUID = 1L;
    public int oldCol, oldRow, newCol, newRow;
    public int promotionType = -1;

    public MovePacket(int oldCol, int oldRow, int newCol, int newRow, int pType) {
        this.oldCol = oldCol;
        this.oldRow = oldRow;
        this.newCol = newCol;
        this.newRow = newRow;
        this.promotionType = pType;
    }
}