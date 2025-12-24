package view;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Dimension;

public class MainFrame extends JFrame {
    public MainFrame() {
        setTitle("Chess Game 2025");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);

        // Kích thước khởi tạo 900x600
        getContentPane().setPreferredSize(new Dimension(900, 600));
        pack();
        setLocationRelativeTo(null);
    }

    public void showPanel(JPanel panel) {
        getContentPane().removeAll();
        add(panel);
        revalidate();
        repaint();
        panel.requestFocusInWindow();
        setVisible(true);
    }
}