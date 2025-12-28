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

    public void hostGame(int port) {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                socket = serverSocket.accept();
                setupStreams();
                sendConfig(new GameConfigPacket(controller.playerColor));
                listenForData();
            } catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    public void joinGame(String ip, int port) {
        new Thread(() -> {
            try {
                socket = new Socket(ip, port);
                setupStreams();
                listenForData();
            } catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    private void setupStreams() throws IOException {
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    public void sendMove(MovePacket packet) {
        try {
            if (out != null) { out.writeObject(packet); out.flush(); }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void sendConfig(GameConfigPacket packet) {
        try {
            if (out != null) { out.writeObject(packet); out.flush(); }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void listenForData() {
        try {
            while (socket != null && !socket.isClosed()) {
                Object data = in.readObject();
                if (data instanceof MovePacket) controller.receiveNetworkMove((MovePacket) data);
                else if (data instanceof GameConfigPacket) controller.onConfigReceived((GameConfigPacket) data);
            }
        } catch (Exception e) { System.out.println("Disconnected."); }
    }
}