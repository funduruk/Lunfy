package ru.funduruk.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ru.funduruk.manager.SceneManager;
import ru.funduruk.model.UserProfile;

import java.io.File;

public class ProfileController extends Controller {

    @FXML public BorderPane rootPane;
    @FXML public HBox titleBar;
    @FXML private ImageView avatarImage;
    @FXML private TextField usernameField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TextArea bioField;

    @FXML
    public void initialize() {
        super.initialize(rootPane, titleBar);

        statusCombo.getItems().addAll("Онлайн", "Не беспокоить", "Офлайн");
        statusCombo.setValue("Онлайн");

        // Загружаем сохранённый профиль если есть
        UserProfile profile = UserProfile.getInstance();
        usernameField.setText(profile.getUsername());
        bioField.setText(profile.getBio());
        statusCombo.setValue(profile.getStatus());
        if (profile.getAvatarPath() != null) {
            avatarImage.setImage(new Image("file:" + profile.getAvatarPath()));
        }
    }

    @FXML
    private void uploadAvatar() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Выбери аватарку");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg")
        );
        File file = chooser.showOpenDialog(avatarImage.getScene().getWindow());
        if (file != null) {
            Image img = new Image("file:" + file.getAbsolutePath());
            avatarImage.setImage(img);
            UserProfile.getInstance().setAvatarPath(file.getAbsolutePath());
        }
    }

    @FXML
    private void save() {
        UserProfile profile = UserProfile.getInstance();
        profile.setUsername(usernameField.getText());
        profile.setBio(bioField.getText());
        profile.setStatus(statusCombo.getValue());
        goBack();
    }

    @FXML
    private void goBack() {
        SceneManager.setScene("/fxml/GeneralView.fxml", "/css/style.css");
    }

    @FXML
    private void close() {
        ((Stage) rootPane.getScene().getWindow()).close();
    }
}