package ru.funduruk.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import ru.funduruk.manager.AudioSettings;

public class SettingsController {

    @FXML private ComboBox<String> micCombo;
    @FXML private ComboBox<String> speakerCombo;
    @FXML private Slider micVolumeSlider;
    @FXML private Slider speakerVolumeSlider;
    @FXML private Label micVolumeLabel;
    @FXML private Label speakerVolumeLabel;

    @FXML
    public void initialize() {
        AudioSettings s = AudioSettings.getInstance();

        micCombo.getItems().add("По умолчанию");
        micCombo.getItems().addAll(AudioSettings.listMicrophones());

        speakerCombo.getItems().add("По умолчанию");
        speakerCombo.getItems().addAll(AudioSettings.listSpeakers());

        if (s.getSelectedMicName() != null) micCombo.setValue(s.getSelectedMicName());
        else micCombo.setValue("По умолчанию");

        if (s.getSelectedSpeakerName() != null) speakerCombo.setValue(s.getSelectedSpeakerName());
        else speakerCombo.setValue("По умолчанию");

        micCombo.valueProperty().addListener((obs, old, val) -> {
            s.setSelectedMicName("По умолчанию".equals(val) ? null : val);
        });
        speakerCombo.valueProperty().addListener((obs, old, val) -> {
            s.setSelectedSpeakerName("По умолчанию".equals(val) ? null : val);
        });

        micVolumeSlider.setValue(s.getMicVolume() * 100);
        speakerVolumeSlider.setValue(s.getSpeakerVolume() * 100);
        micVolumeLabel.setText((int)(s.getMicVolume() * 100) + "%");
        speakerVolumeLabel.setText((int)(s.getSpeakerVolume() * 100) + "%");

        micVolumeSlider.valueProperty().addListener((obs, old, val) -> {
            double v = val.doubleValue() / 100.0;
            s.setMicVolume(v);
            micVolumeLabel.setText((int) val.doubleValue() + "%");
        });
        speakerVolumeSlider.valueProperty().addListener((obs, old, val) -> {
            double v = val.doubleValue() / 100.0;
            s.setSpeakerVolume(v);
            speakerVolumeLabel.setText((int) val.doubleValue() + "%");
        });
    }
}