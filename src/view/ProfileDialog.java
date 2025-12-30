package view;

import javax.swing.*;
import java.awt.*;

public class ProfileDialog extends JDialog {
    public String pName = "Player";
    public int pColor = 0;
    public boolean confirmed = false;

    // lockedColor: -1 nếu là Host (tự chọn), 0 hoặc 1 nếu là Joiner (bị khóa)
    public ProfileDialog(JFrame parent, int lockedColor) {
        super(parent, "Setup Profile", true);
        setLayout(new GridLayout(4, 2, 10, 10)); // Tăng hàng để thêm ghi chú

        add(new JLabel(" Your Name:"));
        JTextField txtName = new JTextField("Player" + (int)(Math.random()*100));
        add(txtName);

        add(new JLabel(" Choose Side:"));
        String[] colors = {"White Circle", "Black Circle"};
        JComboBox<String> cbColor = new JComboBox<>(colors);

        // LOGIC KHÓA MÀU
        if (lockedColor != -1) {
            pColor = (lockedColor == 0) ? 1 : 0; // Lấy màu ngược lại Host
            cbColor.setSelectedIndex(pColor);
            cbColor.setEnabled(false); // Không cho chọn lại
            add(cbColor);
            add(new JLabel("")); // Placeholder
            JLabel lblInfo = new JLabel("<html><font color='orange'>Side auto-picked based on Host</font></html>");
            add(lblInfo);
        } else {
            add(cbColor);
        }

        JButton btnOk = new JButton("Confirm");
        btnOk.addActionListener(e -> {
            pName = txtName.getText();
            pColor = cbColor.getSelectedIndex();
            confirmed = true;
            dispose();
        });

        add(new JLabel("")); // Placeholder
        add(btnOk);

        pack();
        setLocationRelativeTo(parent);
    }
}