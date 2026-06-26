package ru.funduruk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.funduruk.manager.SceneManager;
import ru.funduruk.model.UserProfile;
import ru.funduruk.net.ApiClient;

import java.util.Map;

public class VerifyEmailController {

    @FXML private TextField codeField;
    @FXML private Label subtitleLabel;
    @FXML private Button resendBtn;
    @FXML private VBox statusBox;
    @FXML private Label statusLabel;
    @FXML private HBox titleBar;

    private static String pendingEmail;
    private static String pendingUsername;
    private static String pendingTag;

    public static void setPending(String email, String username, String tag) {
        pendingEmail = email;
        pendingUsername = username;
        pendingTag = tag;
    }

    @FXML
    public void initialize() {
        if (pendingEmail != null) {
            subtitleLabel.setText("Мы отправили код на " + pendingEmail);
        }
        codeField.textProperty().addListener((obs, old, val) -> {
            String filtered = val.replaceAll("\\D", "");
            if (filtered.length() > 6) filtered = filtered.substring(0, 6);
            if (!filtered.equals(val)) codeField.setText(filtered);
        });
    }

    @FXML
    private void handleVerify() {
        String code = codeField.getText().trim();
        if (code.length() != 6) {
            showError("Введите 6-значный код");
            return;
        }

        new Thread(() -> {
            try {
                Map<String, Object> response = ApiClient.post(
                        "/api/auth/verify",
                        Map.of("email", pendingEmail, "code", code)
                );

                Platform.runLater(() -> {
                    if (response.containsKey("error")) {
                        showError((String) response.get("error"));
                        return;
                    }

                    String token = (String) response.get("token");
                    String username = (String) response.get("username");
                    String tag = (String) response.get("tag");

                    ApiClient.setToken(token);
                    ApiClient.setCurrentUsername(username);
                    ApiClient.setCurrentTag(tag);

                    UserProfile.getInstance().setUsername(username);
                    UserProfile.getInstance().setTag(tag);

                    SceneManager.setScene("/fxml/GeneralView.fxml", "/css/general_style.css");
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Нет соединения с сервером"));
            }
        }).start();
    }

    @FXML
    private void handleResend() {
        resendBtn.setDisable(true);
        resendBtn.setText("Отправка...");

        new Thread(() -> {
            try {
                Map<String, Object> response = ApiClient.post(
                        "/api/auth/resend",
                        Map.of("email", pendingEmail)
                );

                Platform.runLater(() -> {
                    if (response.containsKey("error")) {
                        showError((String) response.get("error"));
                        resendBtn.setDisable(false);
                        resendBtn.setText("Отправить код ещё раз");
                    } else {
                        showInfo("Код отправлен на почту");
                        startResendCountdown();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Нет соединения с сервером");
                    resendBtn.setDisable(false);
                    resendBtn.setText("Отправить код ещё раз");
                });
            }
        }).start();
    }

    private void startResendCountdown() {
        new Thread(() -> {
            for (int i = 60; i > 0; i--) {
                int sec = i;
                Platform.runLater(() ->
                        resendBtn.setText("Повторно через " + sec + " сек"));
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
            Platform.runLater(() -> {
                resendBtn.setText("Отправить код ещё раз");
                resendBtn.setDisable(false);
            });
        }).start();
    }

    private void showError(String msg) {
        statusLabel.setStyle("-fx-text-fill: #ff5c7a;");
        statusLabel.setText(msg);
        statusBox.setVisible(true);
        statusBox.setManaged(true);
    }

    private void showInfo(String msg) {
        statusLabel.setStyle("-fx-text-fill: #00ffc8;");
        statusLabel.setText(msg);
        statusBox.setVisible(true);
        statusBox.setManaged(true);
    }

    @FXML
    private void close() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        stage.hide();
        System.exit(0);
    }

    @FXML
    private void minimize() {
        ((Stage) titleBar.getScene().getWindow()).setIconified(true);
    }
}