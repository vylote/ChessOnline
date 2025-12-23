package utility;

import javax.sound.sampled.*;
import java.io.File;

public class AudioManager {
    private Clip bgmClip;
    private String currentPath = ""; // Lưu đường dẫn nhạc đang phát
    private float currentVolume = 0.5f;

    public void playBGM(String filePath) {
        // Nếu file yêu cầu trùng với file đang phát, không làm gì cả (giúp nhạc liền mạch)
        if (filePath.equals(currentPath) && bgmClip != null && bgmClip.isRunning()) {
            return;
        }

        stopBGM(); // Dừng bản nhạc cũ nếu có

        try {
            File audioFile = new File(filePath);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            bgmClip = AudioSystem.getClip();
            bgmClip.open(audioStream);

            currentPath = filePath;
            applyVolume();

            bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
            bgmClip.start();
        } catch (Exception e) {
            System.err.println("Lỗi phát nhạc: " + filePath);
        }
    }

    public void stopBGM() {
        if (bgmClip != null) {
            bgmClip.stop();
            bgmClip.close();
        }
        currentPath = "";
    }

    public void setVolumeFromSlider(int value) {
        this.currentVolume = value / 100.0f;
        applyVolume();
    }

    public int getVolumeAsInt() {
        return Math.round(currentVolume * 100);
    }

    private void applyVolume() {
        if (bgmClip != null && bgmClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gainControl = (FloatControl) bgmClip.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = (float) (Math.log(currentVolume <= 0 ? 0.0001 : currentVolume) / Math.log(10.0) * 20.0);
            gainControl.setValue(dB);
        }
    }
}