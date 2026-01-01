package utility;

import javax.sound.sampled.*;
import java.net.URL;

public class AudioManager {
    private Clip bgmClip;
    private String currentPath = "";

    private float bgmVolume = 0.01f;
    private float sfxVolume = 1f;

    // --- QUẢN LÝ BGM ---
    public void setBGMVolumeFromSlider(int value) {
        this.bgmVolume = value / 100.0f;
        applyBGMVolume();
    }

    public int getBGMVolumeAsInt() { return Math.round(bgmVolume * 100); }

    private void applyBGMVolume() {
        if (bgmClip != null && bgmClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gainControl = (FloatControl) bgmClip.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = (float) (Math.log(bgmVolume <= 0 ? 0.0001 : bgmVolume) / Math.log(10.0) * 20.0);
            gainControl.setValue(dB);
        }
    }

    // --- QUẢN LÝ SFX ---
    public void setSFXVolumeFromSlider(int value) { this.sfxVolume = value / 100.0f; }

    public int getSFXVolumeAsInt() { return Math.round(sfxVolume * 100); }

    public void playSFX(String filePath) {
        try {
            // SỬA ĐỔI: Sử dụng getResource để đọc file từ bên trong JAR
            URL soundURL = getClass().getResource(filePath);
            if (soundURL == null) {
                System.err.println("Không tìm thấy SFX: " + filePath);
                return;
            }

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundURL);
            Clip sfxClip = AudioSystem.getClip();
            sfxClip.open(audioStream);

            if (sfxClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) sfxClip.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = (float) (Math.log(sfxVolume <= 0 ? 0.0001 : sfxVolume) / Math.log(10.0) * 20.0);
                gainControl.setValue(dB);
            }
            sfxClip.start();
            sfxClip.addLineListener(e -> { if (e.getType() == LineEvent.Type.STOP) sfxClip.close(); });
        } catch (Exception e) { System.err.println("Lỗi SFX: " + filePath); }
    }

    public void playBGM(String filePath) {
        if (filePath.equals(currentPath) && bgmClip != null && bgmClip.isRunning()) return;
        stopBGM();
        try {
            // SỬA ĐỔI: Sử dụng getResource cho nhạc nền
            URL soundURL = getClass().getResource(filePath);
            if (soundURL == null) {
                System.err.println("Không tìm thấy BGM: " + filePath);
                return;
            }

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundURL);
            bgmClip = AudioSystem.getClip();
            bgmClip.open(audioStream);
            currentPath = filePath;
            applyBGMVolume();
            bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
            bgmClip.start();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void stopBGM() {
        if (bgmClip != null) { bgmClip.stop(); bgmClip.close(); }
        currentPath = "";
    }
}