import javax.sound.sampled.*;
import java.io.File;

public class Sound {
    private static Clip currentLoop = null; // Для зацикленного звука (шаги)

    // Обычный звук (выстрел, клик, монетка)
    public static void play(String soundName) {
        String[] paths = {
                "src/Sound/" + soundName,
                "Sound/" + soundName,
                "src/asset/Sound/" + soundName,
                "asset/Sound/" + soundName
        };

        for (String path : paths) {
            File soundFile = new File(path);
            if (soundFile.exists()) {
                try {
                    AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioIn);
                    clip.start();
                    return;
                } catch (Exception e) { }
            }
        }
    }

    // Зацикленный звук (шаги)
    public static void loop(String soundName) {
        if (currentLoop != null && currentLoop.isRunning()) return; // Уже играет

        String[] paths = {
                "src/Sound/" + soundName,
                "Sound/" + soundName,
                "src/asset/Sound/" + soundName,
                "asset/Sound/" + soundName
        };

        for (String path : paths) {
            File soundFile = new File(path);
            if (soundFile.exists()) {
                try {
                    AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
                    currentLoop = AudioSystem.getClip();
                    currentLoop.open(audioIn);
                    currentLoop.loop(Clip.LOOP_CONTINUOUSLY); // Зацикливаем
                    currentLoop.start();
                    return;
                } catch (Exception e) { }
            }
        }
    }

    // Остановка зацикленного звука
    public static void stop() {
        if (currentLoop != null) {
            currentLoop.stop();
            currentLoop.close();
            currentLoop = null;
        }
    }
}