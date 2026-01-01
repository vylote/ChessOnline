package controller;

import model.PlayerProfile;
import java.net.*;
import java.util.*;
import java.util.function.Consumer;

public class DiscoveryService {
    private final int UDP_PORT = 8888;
    private final String PREFIX = "CHESS_HOST:";
    private boolean running = false;

    public void startBroadcasting(String name, int color) {
        running = true;
        new Thread(() -> {
            try {
                String msg = PREFIX + name + ":" + color;
                byte[] buf = msg.getBytes();

                while (running) {
                    // Lấy tất cả các card mạng (Wi-Fi, Ethernet, VPN...)
                    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                    while (interfaces.hasMoreElements()) {
                        NetworkInterface networkInterface = interfaces.nextElement();

                        // Bỏ qua card mạng không hoạt động hoặc card ảo loopback
                        if (networkInterface.isLoopback() || !networkInterface.isUp()) continue;

                        for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                            InetAddress broadcast = interfaceAddress.getBroadcast();
                            if (broadcast == null) continue;

                            // Phát trực tiếp vào địa chỉ Broadcast của từng mạng (ví dụ 192.168.1.255)
                            try (DatagramSocket socket = new DatagramSocket()) {
                                socket.setBroadcast(true);
                                DatagramPacket packet = new DatagramPacket(buf, buf.length, broadcast, UDP_PORT);
                                socket.send(packet);
                            } catch (Exception e) { /* Bỏ qua lỗi trên 1 card mạng cụ thể */ }
                        }
                    }
                    Thread.sleep(2000); // 2 giây phát lại 1 lần
                }
            } catch (Exception e) { if(running) e.printStackTrace(); }
        }).start();
    }

    // Giữ nguyên hàm startListening và stop như cũ
    public void startListening(Consumer<PlayerProfile> onFound) {
        running = true;
        new Thread(() -> {
            try (DatagramSocket listenSocket = new DatagramSocket(UDP_PORT)) {
                listenSocket.setSoTimeout(5000);
                byte[] buf = new byte[1024];
                while (running) {
                    try {
                        DatagramPacket p = new DatagramPacket(buf, buf.length);
                        listenSocket.receive(p);
                        String data = new String(p.getData(), 0, p.getLength());
                        if (data.startsWith(PREFIX)) {
                            String[] parts = data.split(":");
                            onFound.accept(new PlayerProfile(parts[1], Integer.parseInt(parts[2]), p.getAddress().getHostAddress()));
                        }
                    } catch (SocketTimeoutException ignored) {}
                }
            } catch (Exception e) { if(running) e.printStackTrace(); }
        }).start();
    }

    public void stop() { running = false; }
}