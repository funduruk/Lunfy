package ru.funduruk.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.funduruk.manager.FieldsManager;
import ru.funduruk.manager.SceneManager;
import ru.funduruk.net.ApiClient;

import java.util.Map;

public class ResetPasswordController {

    @FXML private TextField codeField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private Button eyeButton;
    @FXML private Label subtitleLabel;
    @FXML private VBox statusBox;
    @FXML private Label statusLabel;
    @FXML private HBox titleBar;

    private static String pendingEmail;

    public static void setPendingEmail(String email) {
        pendingEmail = email;
    }

    @FXML
    public void initialize() {
        if (pendingEmail != null) {
            subtitleLabel.setText("Код отправлен на " + pendingEmail);
        }

        codeField.textProperty().addListener((obs, old, val) -> {
            String filtered = val.replaceAll("\\D", "");
            if (filtered.length() > 6) filtered = filtered.substring(0, 6);
            if (!filtered.equals(val)) codeField.setText(filtered);
        });

        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());
    }

    @FXML
    private void togglePasswordVisibility() {
        boolean showing = passwordVisibleField.isVisible();
        passwordVisibleField.setVisible(!showing);
        passwordVisibleField.setManaged(!showing);
        passwordField.setVisible(showing);
        passwordField.setManaged(showing);
        eyeButton.setText(showing ? "👁" : "🙈");
    }

    @FXML
    private void handleReset() {
        String code = codeField.getText().trim();
        String newPassword = passwordField.getText();

        if (code.length() != 6) {
            showError("Введите 6-значный код");
            return;
        }
        if (!FieldsManager.checkPassword(newPassword)) {
            showError("Пароль должен содержать минимум\n8 символов, заглавную букву,\nцифру и спецсимвол");
            return;
        }

        new Thread(() -> {
            try {
                Map<String, Object> result = ApiClient.post(
                        "/api/auth/reset-password",
                        Map.of("email", pendingEmail, "code", code, "password", newPassword)
                );

                Platform.runLater(() -> {
                    if (result.containsKey("error")) {
                        showError((String) result.get("error"));
                        return;
                    }
                    SceneManager.setScene("/fxml/LoginView.fxml", "/css/style.css");
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Нет соединения с сервером"));
            }
        }).start();
    }

    private void showError(String msg) {
        statusLabel.setStyle("-fx-text-fill: #ff5c7a;");
        statusLabel.setText(msg);
        statusBox.setVisible(true);
        statusBox.setManaged(true);
    }

    @FXML
    private void close() {
        ((Stage) titleBar.getScene().getWindow()).close();
    }

    @FXML
    private void minimize() {
        ((Stage) titleBar.getScene().getWindow()).setIconified(true);
    }
}