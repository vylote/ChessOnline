package controller;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Mouse extends MouseAdapter {
    public int x, y;
    public boolean pressed, released;

    public void updateLocation(int lx, int ly) {
        this.x = lx;
        this.y = ly;
    }

    @Override
    public void mousePressed(MouseEvent e) { pressed = true; released = false; }

    @Override
    public void mouseReleased(MouseEvent e) { pressed = false; released = true; }
}