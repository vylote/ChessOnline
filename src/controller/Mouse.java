package controller;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Mouse extends MouseAdapter {
    public int x, y;
    public boolean pressed;
    public boolean released;
    // public boolean released = false; // <<< XÓA DÒNG NÀY (Đã được xử lý trong GameController)

    public void mousePressed(MouseEvent e) {
        pressed = true;

        x = e.getX();
        y = e.getY();
    }

    public void mouseReleased(MouseEvent e) {
        pressed = false;

        // XÓA DÒNG SAU: released = true;
        // Lý do: Giá trị này sẽ được set trong controller.handleMouseRelease()
        // và được đặt lại (reset) sau khi logic game đã xử lý xong.
    }

    public void mouseDragged(MouseEvent e) {
        x = e.getX();
        y = e.getY();
    }
}