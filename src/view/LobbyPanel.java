package view;

import controller.GameController;
import model.PlayerProfile;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class LobbyPanel extends JPanel {
    private final GameController controller;
    private final PlayerProfile myInfo;
    private PlayerProfile opponentInfo;

    private JButton btnAction;
    private JLabel lblStatus;
    private boolean isOpponentConnected = false;

    public LobbyPanel(GameController controller, String myName, int myColor) {
        this.controller = controller;
        this.myInfo = new PlayerProfile(myName, myColor, "Local");

        setLayout(null);
        setBackground(new Color(15, 15, 15));

        // 1. Nhãn trạng thái
        lblStatus = new JLabel(controller.isServer ? "WAITING FOR OPPONENT..." : "CONNECTING TO HOST...", SwingConstants.CENTER);
        lblStatus.setForeground(new Color(150, 150, 150));
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        add(lblStatus);

        // 2. Nút bấm
        btnAction = new JButton();
        btnAction.setFont(new Font("Segoe UI", Font.BOLD, 18));
        btnAction.setFocusPainted(false);
        btnAction.setForeground(Color.WHITE);

        // THIẾT LẬP TRẠNG THÁI BAN ĐẦU
        if (controller.isServer) {
            // Nếu là Host: Khóa nút, đợi người vào
            btnAction.setText("WAITING...");
            btnAction.setBackground(new Color(100, 100, 100));
            btnAction.setEnabled(false);
        } else {
            // Nếu là Joiner: Hiện nút CANCEL để thoát nếu muốn
            btnAction.setText("CANCEL");
            btnAction.setBackground(new Color(200, 50, 50));
            btnAction.setEnabled(true);
        }

        btnAction.addActionListener(e -> {
            if (controller.isServer && isOpponentConnected) {
                // HOST ẤN: Bắt đầu game cho cả 2 máy
                controller.hostPressStart();
            } else {
                // JOINER ẤN hoặc HOST ẤN KHI CHƯA CÓ AI: Thoát
                controller.exitToMenu();
            }
        });
        add(btnAction);

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = getWidth();
                int h = getHeight();
                lblStatus.setBounds(0, (int)(h * 0.7), w, 30);
                int btnW = 200, btnH = 50;
                btnAction.setBounds((w - btnW) / 2, (int)(h * 0.8), btnW, btnH);
            }
        });
    }

    // GỌI HÀM NÀY TỪ CONTROLLER KHI NHẬN ĐƯỢC CONFIG CỦA ĐỐI THỦ
    public void setOpponent(PlayerProfile opp) {
        this.opponentInfo = opp;
        this.isOpponentConnected = true;

        SwingUtilities.invokeLater(() -> {
            if (controller.isServer) {
                // NẾU LÀ HOST: Bật nút START lên
                lblStatus.setText("OPPONENT FOUND: " + opp.name.toUpperCase());
                lblStatus.setForeground(new Color(46, 204, 113));

                btnAction.setText("START GAME");
                btnAction.setBackground(new Color(46, 204, 113)); // Màu xanh lá
                btnAction.setEnabled(true);
            } else {
                // NẾU LÀ JOINER: Vẫn giữ nút CANCEL, chỉ cập nhật tên Host
                lblStatus.setText("CONNECTED TO " + opp.name.toUpperCase() + ". WAITING...");
                lblStatus.setForeground(new Color(52, 152, 219)); // Màu xanh dương

                btnAction.setText("CANCEL");
                btnAction.setBackground(new Color(200, 50, 50));
                btnAction.setEnabled(true);
            }
            repaint();
        });
    }

    // Hàm đếm ngược chuẩn bị vào game (nếu muốn)
    public void startSyncDelay() {
        SwingUtilities.invokeLater(() -> {
            btnAction.setEnabled(false);
            btnAction.setText("STARTING...");
            btnAction.setBackground(new Color(46, 204, 113));
            lblStatus.setText("PREPARING BOARD...");
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int midX = getWidth() / 2;
        int midY = getHeight() / 2 - 60;

        // Vẽ chữ VS mờ
        g2.setFont(new Font("Arial", Font.ITALIC | Font.BOLD, 100));
        g2.setColor(new Color(255, 255, 255, 15));
        g2.drawString("VS", midX - 70, midY + 40);

        // Vẽ Me
        drawPlayer(g2, midX - 220, midY, myInfo.name, myInfo.color, true);

        // Vẽ Opponent
        if (opponentInfo == null) {
            drawEmpty(g2, midX + 220, midY);
        } else {
            drawPlayer(g2, midX + 220, midY, opponentInfo.name, opponentInfo.color, false);
        }
    }

    private void drawPlayer(Graphics2D g2, int x, int y, String name, int color, boolean isLocal) {
        // Vẽ vòng tròn màu quân cờ
        g2.setColor(color == 0 ? Color.WHITE : new Color(50, 50, 50));
        g2.fillOval(x - 70, y - 70, 140, 140);

        // Vẽ viền trạng thái (Xanh lá nếu là mình, Xanh dương nếu là đối thủ)
        g2.setColor(isLocal ? new Color(46, 204, 113) : new Color(52, 152, 219));
        g2.setStroke(new BasicStroke(5));
        g2.drawOval(x - 70, y - 70, 140, 140);

        // Vẽ tên
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(name, x - fm.stringWidth(name) / 2, y + 100);
    }

    private void drawEmpty(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(60, 60, 60));
        float[] dash = {10.0f};
        g2.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
        g2.drawOval(x - 70, y - 70, 140, 140);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 40));
        g2.drawString("?", x - 15, y + 15);
    }
}