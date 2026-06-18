package ru.funduruk.manager;

import javafx.scene.media.AudioClip;

public class CallSoundManager {

    private static AudioClip ringtone;
    private static AudioClip dialing;

    private static AudioClip load(String path) {
        try {
            return new AudioClip(
                    CallSoundManager.class.getResource(path).toExternalForm());
        } catch (Exception e) {
            System.err.println("Не удалось загрузить звук: " + path);
            return null;
        }
    }

    public static void playRingtone() {
        stopAll();
        if (ringtone == null) ringtone = load("/sounds/ringtone.mp3");
        if (ringtone != null) {
            ringtone.setCycleCount(AudioClip.INDEFINITE);
            ringtone.play();
        }
    }

    public static void playDialing() {
        stopAll();
        if (dialing == null) dialing = load("/sounds/dialing.mp3");
        if (dialing != null) {
            dialing.setCycleCount(AudioClip.INDEFINITE);
            dialing.play();
        }
    }

    public static void stopAll() {
        if (ringtone != null) ringtone.stop();
        if (dialing != null) dialing.stop();
    }
}