package view;

import controller.GameController;
import model.PlayerProfile;
import javax.swing.*;
import java.awt.*;

public class LobbyPanel extends JPanel {
    private PlayerProfile myInfo;
    private PlayerProfile opponentInfo;

    public LobbyPanel(String myName, int myColor) {
        this.myInfo = new PlayerProfile(myName, myColor, "127.0.0.1");
        setBackground(new Color(15, 15, 15));
    }

    public void setOpponent(PlayerProfile opp) {
        this.opponentInfo = opp;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int midX = getWidth() / 2;
        int midY = getHeight() / 2;

        // Vẽ Bạn (Bên trái)
        drawPlayer(g2, midX - 200, midY, myInfo.name, myInfo.color);

        // Vẽ VS
        g2.setFont(new Font("Arial", Font.ITALIC | Font.BOLD, 60));
        g2.setColor(new Color(255, 255, 255, 50));
        g2.drawString("VS", midX - 40, midY + 20);

        // Vẽ Đối thủ (Bên phải)
        if (opponentInfo == null) {
            drawEmpty(g2, midX + 200, midY);
        } else {
            drawPlayer(g2, midX + 200, midY, opponentInfo.name, opponentInfo.color);
        }
    }

    private void drawPlayer(Graphics2D g2, int x, int y, String name, int color) {
        g2.setColor(color == GameController.WHITE ? Color.WHITE : new Color(60, 60, 60));
        g2.fillOval(x - 60, y - 60, 120, 120);

        g2.setColor(new Color(0, 255, 150));
        g2.setStroke(new BasicStroke(3));
        g2.drawOval(x - 60, y - 60, 120, 120);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
        g2.drawString(name, x - g2.getFontMetrics().stringWidth(name)/2, y + 90);
    }

    private void drawEmpty(Graphics2D g2, int x, int y) {
        g2.setColor(Color.DARK_GRAY);
        g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f}, 0.0f));
        g2.drawOval(x - 60, y - 60, 120, 120);
        g2.drawString("?", x - 10, y + 15);
    }
}