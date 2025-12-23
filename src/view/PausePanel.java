package view;

import controller.GameController;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

public class PausePanel extends JPanel {

    private final GameController controller;
    private BufferedImage backgroundSnapshot;

    // --- CẤU HÌNH SỐ LƯỢNG SLOT ---
    private static final int TOTAL_SLOTS = 4;

    // --- HẰNG SỐ BỐ CỤC (Dựa trên kích thước 1200x800) ---
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;

    // Vùng bên trái (Slots)
    private final Rectangle[] slotBackgroundRects = new Rectangle[TOTAL_SLOTS];
    // Chia mỗi slot thành 2 vùng click: nửa trên Save, nửa dưới Load
    private final Rectangle[] saveClickRects = new Rectangle[TOTAL_SLOTS];
    private final Rectangle[] loadClickRects = new Rectangle[TOTAL_SLOTS];

    // Vùng bên phải (Menu)
    private final String[] menuItems = {"Continue", "Game settings", "Exit to menu"};
    private final Rectangle[] menuRects = new Rectangle[menuItems.length];

    // --- TRẠNG THÁI HOVER ---
    private int hoveredSlotIndex = -1;
    private boolean isHoveringSave = false; // true nếu đang hover vùng Save, false nếu Load
    private int hoveredMenuIndex = -1;

    // Font chữ
    private final Font numberFont = new Font("Arial", Font.BOLD, 50);
    private final Font stateFont = new Font("Arial", Font.PLAIN, 22);
    private final Font dateFont = new Font("Monospaced", Font.PLAIN, 20);
    private final Font menuFont = new Font("Arial", Font.PLAIN, 28);

    public PausePanel(GameController gc, JFrame containingFrame) {
        this.controller = gc;
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setLayout(null);

        initLayout();

        // --- XỬ LÝ CHUỘT ---
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleMouseHover(e.getX(), e.getY());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleMouseClick(e.getX(), e.getY());
            }
        };
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    /** Khởi tạo tọa độ cho các vùng vẽ và click */
    private void initLayout() {
        // 1. Setup khu vực Slots (Bên trái)
        int slotX = 50;
        int slotYStart = 100;
        int slotWidth = 750;
        int slotHeight = 110;
        int slotSpacing = 20;
        int numberBoxWidth = 100; // Chiều rộng hộp chứa số thứ tự

        for (int i = 0; i < TOTAL_SLOTS; i++) {
            int y = slotYStart + i * (slotHeight + slotSpacing);

            // Vùng nền tổng thể của slot
            slotBackgroundRects[i] = new Rectangle(slotX, y, slotWidth, slotHeight);

            // Vùng chứa text Save/Load (bên phải số thứ tự)
            int textAreaX = slotX + numberBoxWidth;
            int textAreaWidth = slotWidth - numberBoxWidth;

            // Chia đôi chiều cao cho vùng Save (trên) và Load (dưới)
            saveClickRects[i] = new Rectangle(textAreaX, y, textAreaWidth, slotHeight / 2);
            loadClickRects[i] = new Rectangle(textAreaX, y + slotHeight / 2, textAreaWidth, slotHeight / 2);
        }

        // 2. Setup khu vực Menu (Bên phải)
        int menuX = 850;
        int menuYStart = 100;
        int menuItemHeight = 50;
        int menuSpacing = 40;

        for (int i = 0; i < menuItems.length; i++) {
            int y = menuYStart + i * (menuItemHeight + menuSpacing);
            // Tạo vùng click rộng để dễ bấm
            menuRects[i] = new Rectangle(menuX, y, 300, menuItemHeight);
        }
    }

    public void setBackgroundSnapshot(BufferedImage snapshot) {
        this.backgroundSnapshot = snapshot;
    }

    private void handleMouseHover(int x, int y) {
        hoveredSlotIndex = -1;
        hoveredMenuIndex = -1;
        isHoveringSave = false;

        // Kiểm tra hover slots
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (saveClickRects[i].contains(x, y)) {
                hoveredSlotIndex = i;
                isHoveringSave = true;
                break;
            } else if (loadClickRects[i].contains(x, y)) {
                hoveredSlotIndex = i;
                isHoveringSave = false;
                break;
            }
        }

        // Kiểm tra hover menu phải
        if (hoveredSlotIndex == -1) {
            for (int i = 0; i < menuItems.length; i++) {
                if (menuRects[i].contains(x, y)) {
                    hoveredMenuIndex = i;
                    break;
                }
            }
        }

        // --- THAY ĐỔI CON CHUỘT TẠI ĐÂY ---
        if (hoveredSlotIndex != -1 || hoveredMenuIndex != -1) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
        repaint();
    }

    private void handleMouseClick(int x, int y) {
        // Xử lý click Slot
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            int slotNum = i + 1;
            if (saveClickRects[i].contains(x, y)) {
                controller.saveGame(slotNum);
                repaint(); // Cập nhật lại ngày giờ sau khi lưu
                return;
            } else if (loadClickRects[i].contains(x, y)) {
                controller.loadGame(slotNum);
                return;
            }
        }

        // Xử lý click Menu phải
        if (menuRects[0].contains(x, y)) controller.resumeGame();
        else if (menuRects[1].contains(x, y)) System.out.println("Settings clicked");
        else if (menuRects[2].contains(x, y)) controller.exitToMenu();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Vẽ nền game và lớp phủ tối
        if (backgroundSnapshot != null) {
            g2.drawImage(backgroundSnapshot, 0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, null);
        }
        g2.setColor(new Color(0, 0, 0, 180)); // Lớp phủ màu đen bán trong suốt
        g2.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        // 2. Vẽ các Slots bên trái
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            drawSlot(g2, i);
        }

        // 3. Vẽ Menu bên phải
        drawRightMenu(g2);
    }

    private void drawSlot(Graphics2D g2, int index) {
        Rectangle bgRect = slotBackgroundRects[index];
        Rectangle saveRect = saveClickRects[index];
        Rectangle loadRect = loadClickRects[index];

        // Lấy dữ liệu slot
        String metadata = controller.getSlotMetadata(index + 1);
        boolean hasData = !metadata.equals("Empty Slot") && !metadata.equals("Error Data");

        // --- VẼ NỀN SLOT ---
        // Màu nền mặc định: Đen xám trong suốt
        g2.setColor(new Color(50, 50, 50, 200));
        g2.fillRect(bgRect.x, bgRect.y, bgRect.width, bgRect.height);

        // Hiệu ứng Hover: Làm sáng vùng Save hoặc Load đang được chọn
        if (hoveredSlotIndex == index) {
            g2.setColor(new Color(100, 100, 100, 150)); // Màu sáng hơn cho hover
            if (isHoveringSave) {
                g2.fillRect(saveRect.x, saveRect.y, saveRect.width, saveRect.height);
            } else if (hasData) { // Chỉ hover vùng Load nếu có dữ liệu
                g2.fillRect(loadRect.x, loadRect.y, loadRect.width, loadRect.height);
            }
        }

        // --- VẼ SỐ THỨ TỰ (Cột đầu tiên) ---
        g2.setColor(Color.WHITE);
        g2.setFont(numberFont);
        // Căn giữa số trong hộp bên trái
        int numberBoxCenter = bgRect.x + 50;
        int textY = bgRect.y + bgRect.height / 2 + 20;
        g2.drawString(String.valueOf(index + 1), numberBoxCenter - 15, textY);

        // VẼ ĐƯỜNG KẺ DỌC NGĂN CÁCH SỐ VÀ TEXT
        g2.setColor(new Color(255, 255, 255, 100));
        g2.drawLine(bgRect.x + 100, bgRect.y, bgRect.x + 100, bgRect.y + bgRect.height);

        // --- VẼ TEXT SAVE / LOAD (Cột thứ hai) ---
        int textX = saveRect.x + 20;

        // Dòng trên: Save state
        g2.setFont(stateFont);
        g2.setColor((hoveredSlotIndex == index && isHoveringSave) ? Color.YELLOW : Color.WHITE);
        g2.drawString("Save state", textX, saveRect.y + 35);

        // Dòng dưới: Load state + Ngày giờ (Chỉ vẽ nếu có dữ liệu)
        if (hasData) {
            g2.setFont(stateFont);
            g2.setColor((hoveredSlotIndex == index && !isHoveringSave) ? Color.YELLOW : Color.WHITE);
            g2.drawString("Load state", textX, loadRect.y + 35);

            // Vẽ ngày giờ bên cạnh (Màu nhạt hơn)
            g2.setFont(dateFont);
            g2.setColor(new Color(200, 200, 200));
            // Tách ngày và giờ để hiển thị cho đẹp nếu cần, ở đây vẽ thẳng
            g2.drawString(metadata, textX + 130, loadRect.y + 35);
        }
    }

    private void drawRightMenu(Graphics2D g2) {
        g2.setFont(menuFont);
        FontMetrics fm = g2.getFontMetrics();

        for (int i = 0; i < menuItems.length; i++) {
            Rectangle r = menuRects[i];
            boolean isHovered = (hoveredMenuIndex == i);

            g2.setColor(isHovered ? Color.YELLOW : Color.WHITE);
            // Căn lề trái cho text menu
            g2.drawString(menuItems[i], r.x, r.y + fm.getAscent());

            // Vẽ thêm dấu mũi tên nếu hover để giống style game
            if (isHovered) {
                g2.drawString(">", r.x - 30, r.y + fm.getAscent());
            }
        }
    }
}