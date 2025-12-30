package view;

import javax.swing.*;
import java.awt.*;

public class ProfileDialog extends JDialog {
    public String pName = "Player";
    public int pColor = 0; // Mặc định Trắng
    public boolean confirmed = false;

    public ProfileDialog(JFrame parent) {
        super(parent, "Setup Profile", true);
        setLayout(new GridLayout(3, 2, 10, 10));

        add(new JLabel(" Your Name:"));
        JTextField txtName = new JTextField("Player" + (int)(Math.random()*100));
        add(txtName);

        add(new JLabel(" Choose Side:"));
        String[] colors = {"White Circle", "Black Circle"};
        JComboBox<String> cbColor = new JComboBox<>(colors);
        add(cbColor);

        JButton btnOk = new JButton("Confirm");
        btnOk.addActionListener(e -> {
            pName = txtName.getText();
            pColor = cbColor.getSelectedIndex();
            confirmed = true;
            dispose();
        });
        add(btnOk);

        setSize(300, 150);
        setLocationRelativeTo(parent);
    }
}