package controller.core;

import model.Piece;
import model.SaveData;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;

public class PersistenceService {
    public void save(int slot, ArrayList<Piece> pieces, int color, int time, BufferedImage img) {
        try {
            File dir = new File("saves");
            if (!dir.exists()) dir.mkdirs();
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(dir, "save_" + slot + ".dat")));
            oos.writeObject(new SaveData(pieces, color, time));
            oos.close();
            if (img != null) ImageIO.write(img, "png", new File(dir, "thumb_" + slot + ".png"));
        } catch (Exception e) { e.printStackTrace(); }
    }

    public SaveData load(int slot) {
        try {
            File f = new File("saves/save_" + slot + ".dat");
            if (!f.exists()) return null;
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
            SaveData data = (SaveData) ois.readObject();
            ois.close();
            return data;
        } catch (Exception e) { return null; }
    }

    public BufferedImage getThumbnail(int s) {
        try { return ImageIO.read(new File("saves/thumb_" + s + ".png")); }
        catch (Exception e) { return null; }
    }

    public String getMetadata(int s) {
        return new File("saves/save_" + s + ".dat").exists() ? "Saved Game" : "Empty Slot";
    }
}