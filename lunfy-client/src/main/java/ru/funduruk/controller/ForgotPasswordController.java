package ru.funduruk.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import ru.funduruk.manager.FieldsManager;
import ru.funduruk.manager.SceneManager;
import ru.funduruk.net.ApiClient;

import java.util.Map;

public class ForgotPasswordController {

    @FXML private TextField emailField;
    @FXML private VBox statusBox;
    @FXML private Label statusLabel;
    @FXML private HBox titleBar;
    @FXML private StackPane backButton;
    @FXML private Circle backBg;

    @FXML
    public void initialize() {
        backButton.setOnMouseClicked(e ->
                SceneManager.setScene("/fxml/LoginView.fxml", "/css/style.css"));
        backButton.setOnMouseEntered(e -> backBg.setOpacity(1));
        backButton.setOnMouseExited(e -> backBg.setOpacity(0));
    }

    @FXML
    private void handleSend() {
        String email = emailField.getText().trim();

        if (!FieldsManager.checkEmail(email)) {
            showError("Введите корректный email");
            return;
        }

        new Thread(() -> {
            try {
                Map<String, Object> result = ApiClient.post(
                        "/api/auth/forgot-password",
                        Map.of("email", email)
                );

                Platform.runLater(() -> {
                    if (result.containsKey("error")) {
                        showError((String) result.get("error"));
                        return;
                    }
                    ResetPasswordController.setPendingEmail(email);
                    SceneManager.setScene("/fxml/ResetPasswordView.fxml", "/css/style.css");
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