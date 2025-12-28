package model;

import java.io.Serializable;

public class MovePacket implements Serializable {
    private static final long serialVersionUID = 1L;
    public int oldCol, oldRow, newCol, newRow;

    public MovePacket(int oldCol, int oldRow, int newCol, int newRow) {
        this.oldCol = oldCol;
        this.oldRow = oldRow;
        this.newCol = newCol;
        this.newRow = newRow;
    }
}