package view;

import controller.DiscoveryService;
import model.PlayerProfile;
import javax.swing.*;
import java.awt.*;
import javax.swing.DefaultListModel; // ĐÚNG

public class InviteDialog extends JDialog {
    private DefaultListModel<PlayerProfile> listModel = new DefaultListModel<>();
    private JList<PlayerProfile> hostList = new JList<>(listModel);
    private DiscoveryService discovery = new DiscoveryService();
    public PlayerProfile selectedHost = null;

    public InviteDialog(JFrame parent) {
        super(parent, "Available Matches (LAN/Radmin)", true);
        setLayout(new BorderLayout());

        // Tùy biến cách hiển thị từng dòng trong danh sách
        hostList.setCellRenderer(new HostCellRenderer());
        hostList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        hostList.setBackground(new Color(30, 30, 30));

        add(new JScrollPane(hostList), BorderLayout.CENTER);

        // Nút kết nối thủ công (Manual IP) nếu không tìm thấy tự động
        JButton btnManual = new JButton("Connect via Manual IP");
        btnManual.addActionListener(e -> {
            String ip = JOptionPane.showInputDialog(this, "Enter Host IP:");
            if (ip != null && !ip.isEmpty()) {
                selectedHost = new PlayerProfile("Manual Player", 0, ip);
                dispose();
            }
        });

        JButton btnJoin = new JButton("Join Selected");
        btnJoin.addActionListener(e -> {
            selectedHost = hostList.getSelectedValue();
            if (selectedHost != null) dispose();
        });

        JPanel southPanel = new JPanel();
        southPanel.add(btnManual);
        southPanel.add(btnJoin);
        add(southPanel, BorderLayout.SOUTH);

        // Bắt đầu lắng nghe UDP
        discovery.startListening(profile -> {
            SwingUtilities.invokeLater(() -> {
                // Nếu IP này chưa có trong danh sách thì thêm vào
                boolean exists = false;
                for (int i = 0; i < listModel.size(); i++) {
                    if (listModel.get(i).ip.equals(profile.ip)) exists = true;
                }
                if (!exists) listModel.addElement(profile);
            });
        });

        setSize(400, 300);
        setLocationRelativeTo(parent);
    }

    @Override
    public void dispose() {
        discovery.stop(); // Dừng nghe UDP khi đóng bảng
        super.dispose();
    }

    // Lớp vẽ tùy chỉnh cho từng dòng trong danh sách
    private class HostCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            PlayerProfile p = (PlayerProfile) value;
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            panel.setBackground(isSelected ? new Color(50, 50, 50) : new Color(30, 30, 30));

            // Vẽ hình tròn Trắng/Đen nhỏ đại diện
            JLabel avt = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(p.color == 0 ? Color.WHITE : Color.BLACK);
                    g2.fillOval(2, 2, 16, 16);
                    g2.setColor(Color.GRAY);
                    g2.drawOval(2, 2, 16, 16);
                }
                @Override
                public Dimension getPreferredSize() { return new Dimension(20, 20); }
            };

            JLabel name = new JLabel(p.name + " (" + p.ip + ")");
            name.setForeground(Color.WHITE);
            name.setFont(new Font("Segoe UI", Font.BOLD, 14));

            panel.add(avt);
            panel.add(name);
            return panel;
        }
    }
}