package model;

import java.io.*;
import java.net.*;
import javax.swing.SwingUtilities;
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

    public void hostGame(int port) {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(port));
                socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                setupStreams();

                // Gửi cấu hình cho Joiner
                sendConfig(new GameConfigPacket(controller.playerColor, controller.getMyName()));

                SwingUtilities.invokeLater(() -> {
                    controller.startNewGame();
                    controller.isTimeRunning = true;
                });

                listenForData();
            } catch (IOException e) {
                System.err.println("Lỗi Server: " + e.getMessage());
            }
        }).start();
    }

    public void joinGame(String ip, int port) {
        new Thread(() -> {
            try {
                socket = new Socket(ip, port);
                socket.setTcpNoDelay(true);
                setupStreams();
                listenForData();
            } catch (IOException e) {
                System.err.println("Không thể kết nối: " + e.getMessage());
            }
        }).start();
    }

    private void setupStreams() throws IOException {
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
    }

    public void sendMove(MovePacket packet) {
        new Thread(() -> {
            try {
                if (out != null) {
                    out.writeObject(packet);
                    out.flush();
                    out.reset();
                }
            } catch (IOException e) {
                System.err.println("Lỗi gửi nước đi: " + e.getMessage());
            }
        }).start();
    }

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

    private void listenForData() {
        try {
            while (socket != null && !socket.isClosed()) {
                Object data = in.readObject();
                // SỬA LỖI: Đẩy xử lý logic vào Luồng giao diện (EDT)
                SwingUtilities.invokeLater(() -> {
                    if (data instanceof MovePacket) {
                        controller.receiveNetworkMove((MovePacket) data);
                    } else if (data instanceof GameConfigPacket) {
                        controller.onConfigReceived((GameConfigPacket) data);
                    }
                });
            }
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> controller.exitToMenu());
        } finally {
            closeConnection();
        }
    }

    public void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}
    }
}