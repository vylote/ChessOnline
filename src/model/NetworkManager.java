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
    public void hostGame(int port) {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(port));

                System.out.println("Server đang chờ kết nối tại cổng: " + port);
                socket = serverSocket.accept();

                // TỐI ƯU 1: Tắt thuật toán Nagle để gửi gói tin ngay lập tức (Giảm trễ)
                socket.setTcpNoDelay(true);

                setupStreams();

                // 1. Gửi cấu hình cho Client
                sendConfig(new GameConfigPacket(controller.playerColor, controller.getMyName()));

                // 2. Chạy trên luồng UI để vào game
                javax.swing.SwingUtilities.invokeLater(() -> {
                    controller.startNewGame();
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

                // TỐI ƯU 1: Tắt thuật toán Nagle
                socket.setTcpNoDelay(true);

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
        // Luôn khởi tạo Output và Flush trước để tránh Deadlock
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
    }

    /** Gửi thông tin nước đi hoặc lệnh điều khiển */
    public void sendMove(MovePacket packet) {
        new Thread(() -> { // Gửi trên luồng riêng để không làm khựng UI
            try {
                if (out != null) {
                    out.writeObject(packet);
                    out.flush();
                    // TỐI ƯU 2: Xóa cache stream sau khi flush để dữ liệu luôn mới và nhẹ
                    out.reset();
                }
            } catch (IOException e) {
                System.err.println("Lỗi gửi dữ liệu nước đi: " + e.getMessage());
            }
        }).start();
    }

    /** Gửi cấu hình ban đầu (Màu sắc, thời gian...) */
    public void sendConfig(GameConfigPacket packet) {
        try {
            if (out != null) {
                out.writeObject(packet);
                out.flush();
                out.reset();
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
                    controller.receiveNetworkMove((MovePacket) data);
                }
                else if (data instanceof GameConfigPacket) {
                    controller.onConfigReceived((GameConfigPacket) data);
                }
            }
        } catch (Exception e) {
            System.out.println("Đối thủ đã ngắt kết nối.");
            javax.swing.SwingUtilities.invokeLater(() -> controller.exitToMenu());
        } finally {
            closeConnection();
        }
    }

    // =========================================================
    // 3. GIẢI PHÓNG TÀI NGUYÊN
    // =========================================================

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