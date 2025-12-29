package model;

import java.io.*;
import java.net.*;
import controller.GameController;

public class NetworkManager {
    private GameController controller;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Socket socket;
    private ServerSocket serverSocket;

    public NetworkManager(GameController controller) {
        this.controller = controller;
    }

    // =========================================================
    // 1. KẾT NỐI (HOST & JOIN)
    // =========================================================

    /** Khởi tạo Server (Host) */
    /** Khởi tạo Server (Host) */
    public void hostGame(int port) {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(port));

                System.out.println("Server đang chờ kết nối tại cổng: " + port);
                socket = serverSocket.accept();
                setupStreams();

                // 1. Gửi cấu hình cho Client
                sendConfig(new GameConfigPacket(controller.playerColor));

                // 2. Chạy trên luồng UI để vào game
                javax.swing.SwingUtilities.invokeLater(() -> {
                    controller.getMenuPanel().closeWaitingDialog();
                    controller.startNewGame();

                    // CHỖ CẦN SỬA: Kích hoạt đồng hồ cho Host sau khi game khởi tạo xong
                    controller.isTimeRunning = true;
                });

                listenForData();
            } catch (IOException e) {
                if (!e.getMessage().contains("Socket closed")) {
                    System.err.println("Lỗi Server: " + e.getMessage());
                }
            }
        }).start();
    }

    /** Kết nối tới Server (Join) */
    public void joinGame(String ip, int port) {
        new Thread(() -> {
            try {
                socket = new Socket(ip, port);
                setupStreams();
                System.out.println("Đã kết nối tới Server: " + ip);
                listenForData();
            } catch (IOException e) {
                System.err.println("Không thể kết nối tới Server: " + e.getMessage());
            }
        }).start();
    }

    // =========================================================
    // 2. TRUYỀN TẢI DỮ LIỆU (SEND & LISTEN)
    // =========================================================

    /** Thiết lập luồng dữ liệu Object */
    private void setupStreams() throws IOException {
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    /** Gửi thông tin nước đi hoặc lệnh điều khiển (Rematch) */
    public void sendMove(MovePacket packet) {
        try {
            if (out != null) {
                out.reset(); // Thêm dòng này để xóa cache Object cũ
                out.writeObject(packet);
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("Lỗi gửi dữ liệu nước đi: " + e.getMessage());
        }
    }

    /** Gửi cấu hình ban đầu (Màu sắc, thời gian...) */
    public void sendConfig(GameConfigPacket packet) {
        try {
            if (out != null) {
                out.writeObject(packet);
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("Lỗi gửi cấu hình: " + e.getMessage());
        }
    }

    /** Luồng lắng nghe dữ liệu từ đối thủ */
    private void listenForData() {
        try {
            while (socket != null && !socket.isClosed()) {
                Object data = in.readObject();

                if (data instanceof MovePacket) {
                    // Controller sẽ xử lý nước đi hoặc các tọa độ đặc biệt (-1, -2)
                    controller.receiveNetworkMove((MovePacket) data);
                }
                else if (data instanceof GameConfigPacket) {
                    controller.onConfigReceived((GameConfigPacket) data);
                }
            }
        } catch (Exception e) {
            System.out.println("Đối thủ đã ngắt kết nối.");
            // Tự động thoát về Menu khi mất kết nối đột ngột
            javax.swing.SwingUtilities.invokeLater(() -> controller.exitToMenu());
        } finally {
            closeConnection();
        }
    }

    // =========================================================
    // 3. GIẢI PHÓNG TÀI NGUYÊN
    // =========================================================

    /** Đóng toàn bộ kết nối và luồng */
    public void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            if (serverSocket != null) serverSocket.close();
            System.out.println("Hệ thống mạng đã đóng sạch sẽ.");
        } catch (IOException e) {
            System.err.println("Lỗi khi đóng kết nối: " + e.getMessage());
        } finally {
            in = null; out = null; socket = null; serverSocket = null;
        }
    }
}