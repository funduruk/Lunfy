package ru.funduruk.manager;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import java.util.ArrayList;
import java.util.List;

public class AudioSettings {

    private static AudioSettings instance;
    public static AudioSettings getInstance() {
        if (instance == null) instance = new AudioSettings();
        return instance;
    }

    private String selectedMicName;
    private String selectedSpeakerName;

    private double micVolume = 1.0;
    private double speakerVolume = 1.0;

    public String getSelectedMicName() { return selectedMicName; }
    public void setSelectedMicName(String n) { this.selectedMicName = n; }

    public String getSelectedSpeakerName() { return selectedSpeakerName; }
    public void setSelectedSpeakerName(String n) { this.selectedSpeakerName = n; }

    public double getMicVolume() { return micVolume; }
    public void setMicVolume(double v) { this.micVolume = Math.max(0, Math.min(1, v)); }

    public double getSpeakerVolume() { return speakerVolume; }
    public void setSpeakerVolume(double v) { this.speakerVolume = Math.max(0, Math.min(1, v)); }

    public static List<String> listMicrophones() {
        List<String> result = new ArrayList<>();
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            Mixer m = AudioSystem.getMixer(info);
            Line.Info[] targetLines = m.getTargetLineInfo();
            for (Line.Info li : targetLines) {
                if (TargetDataLine.class.isAssignableFrom(li.getLineClass())) {
                    result.add(info.getName());
                    break;
                }
            }
        }
        return result;
    }

    public static List<String> listSpeakers() {
        List<String> result = new ArrayList<>();
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            Mixer m = AudioSystem.getMixer(info);
            Line.Info[] sourceLines = m.getSourceLineInfo();
            for (Line.Info li : sourceLines) {
                if (SourceDataLine.class.isAssignableFrom(li.getLineClass())) {
                    result.add(info.getName());
                    break;
                }
            }
        }
        return result;
    }

    public static Mixer.Info findMixerInfo(String name) {
        if (name == null) return null;
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            if (info.getName().equals(name)) return info;
        }
        return null;
    }
}