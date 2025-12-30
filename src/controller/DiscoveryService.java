package controller;

import model.PlayerProfile;

import java.net.*;
import java.util.function.Consumer;

public class DiscoveryService {
    private final int UDP_PORT = 8888;
    private DatagramSocket socket;
    private boolean running = false;

    // HOST: Phát tín hiệu "Tôi đang chờ đấu"
    public void startBroadcasting(String name, int color) {
        running = true;
        new Thread(() -> {
            try {
                socket = new DatagramSocket();
                socket.setBroadcast(true);
                String msg = "CHESS_LOBBY:" + name + ":" + color;
                while (running) {
                    byte[] buf = msg.getBytes();
                    DatagramPacket packet = new DatagramPacket(buf, buf.length,
                            InetAddress.getByName("255.255.255.255"), UDP_PORT);
                    socket.send(packet);
                    Thread.sleep(2000); // 2 giây rao 1 lần
                }
            } catch (Exception e) { if(running) e.printStackTrace(); }
        }).start();
    }

    // JOINER: Lắng nghe danh sách các Host đang mở phòng
    public void startListening(Consumer<PlayerProfile> onFound) {
        new Thread(() -> {
            try (DatagramSocket listenSocket = new DatagramSocket(UDP_PORT)) {
                byte[] buf = new byte[1024];
                while (true) {
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    listenSocket.receive(p);
                    String data = new String(p.getData(), 0, p.getLength());
                    if (data.startsWith("CHESS_LOBBY:")) {
                        String[] parts = data.split(":");
                        onFound.accept(new PlayerProfile(parts[1],
                                Integer.parseInt(parts[2]),
                                p.getAddress().getHostAddress()));
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    public void stop() { running = false; if(socket != null) socket.close(); }
}