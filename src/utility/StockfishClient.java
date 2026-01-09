package utility;

import java.io.*;

public class StockfishClient {
    private Process engineProcess;
    private BufferedReader reader;
    private OutputStreamWriter writer;

    // Trong utility.StockfishClient.java
    public boolean startEngine(String path) {
        try {
            File file = new File(path);
            if (!file.exists()) {
                System.err.println("LỖI: Không tìm thấy file Stockfish tại: " + file.getAbsolutePath());
                return false;
            }

            engineProcess = new ProcessBuilder(path).start(); // Khởi chạy tiến trình
            reader = new BufferedReader(new InputStreamReader(engineProcess.getInputStream()));
            writer = new OutputStreamWriter(engineProcess.getOutputStream());

            // Kích hoạt giao thức UCI ngay khi khởi động
            sendCommand("uci");
            sendCommand("isready");

            System.out.println("AI Stockfish đã sẵn sàng!");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void sendCommand(String cmd) {
        // Thêm kiểm tra null để tránh lỗi bạn vừa gặp
        if (writer == null) {
            System.err.println("LỖI: Writer bị null. Kiểm tra xem engine đã khởi động thành công chưa!");
            return;
        }
        try {
            writer.write(cmd + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Đọc kết quả từ Stockfish để tìm "bestmove"
    public String getBestMove(String fen, int waitTime) {
        sendCommand("position fen " + fen); // Gửi trạng thái bàn cờ
        sendCommand("go movetime " + waitTime); // Ra lệnh tính toán

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("bestmove")) {
                    return line.split(" ")[1]; // Trả về ví dụ: "e2e4"
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void stopEngine() {
        sendCommand("quit");
        engineProcess.destroy();
    }
}
