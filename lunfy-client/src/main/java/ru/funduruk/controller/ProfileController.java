package ru.funduruk.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ru.funduruk.dialog.AvatarEditorDialog;
import ru.funduruk.manager.SceneManager;
import ru.funduruk.model.UserProfile;
import ru.funduruk.net.ApiClient;

import java.io.File;

public class ProfileController extends Controller {

    @FXML public BorderPane rootPane;
    @FXML public HBox titleBar;
    @FXML private Label avatarInitial;
    @FXML private Label displayNameLabel;
    @FXML private Label tagLabel;
    @FXML private TextField usernameField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TextArea bioField;

    @FXML
    public void initialize() {
        super.initialize(rootPane, titleBar);

        statusCombo.getItems().addAll("Онлайн", "Не беспокоить", "Офлайн");
        statusCombo.setValue("Онлайн");

        UserProfile profile = UserProfile.getInstance();
        String username = profile.getUsername();

        usernameField.setText(username);
        bioField.setText(profile.getBio());
        statusCombo.setValue(profile.getStatus());
        displayNameLabel.setText(username);
        tagLabel.setText("#" + profile.getTag());

        if (username != null && !username.isEmpty()) {
            avatarInitial.setText(username.substring(0, 1).toUpperCase());
        }

        loadAvatar(username);

        // color status
        updateStatusDot(profile.getStatus());

        statusCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateStatusDot(newVal));
    }

    @FXML private ImageView avatarImage;

    private void loadAvatar(String username) {
        try {
            String url = ApiClient.HTTP_BASE + "/api/users/" + username + "/avatar";
            Image img = new Image(url, 100, 100, true, true, true);
            img.progressProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() >= 1.0 && !img.isError()) {
                    avatarImage.setImage(img);
                    Circle clip = new Circle(50, 50, 50);
                    avatarImage.setClip(clip);
                    avatarInitial.setVisible(false);
                }
            });
        } catch (Exception ignored) {}
    }

    @FXML private Circle statusDot;

    private void updateStatusDot(String status) {
        if (status == null) return;
        switch (status) {
            case "Онлайн" -> statusDot.setFill(Color.web("#3ba55d"));
            case "Не беспокоить" -> statusDot.setFill(Color.web("#ed4245"));
            default -> statusDot.setFill(Color.web("#747f8d"));
        }
    }

    @FXML private StackPane avatarContainer;

    @FXML
    private void uploadAvatar() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Выбери аватарку");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg")
        );
        File file = chooser.showOpenDialog(avatarContainer.getScene().getWindow());
        if (file == null) return;

        AvatarEditorDialog editor = new AvatarEditorDialog(file);
        File editedFile = editor.showAndWait();
        if (editedFile == null) return;

        new Thread(() -> {
            try {
                String boundary = "----FormBoundary" + System.currentTimeMillis();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(ApiClient.HTTP_BASE + "/api/users/me/avatar"))
                        .header("Authorization", "Bearer " + ApiClient.getToken())
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(buildMultipartBody(editedFile, boundary))
                        .build();

                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpResponse<String> response = client.send(request,
                        java.net.http.HttpResponse.BodyHandlers.ofString());

                System.out.println("User avatar uploaded: " + response.body());

                javafx.application.Platform.runLater(() -> {
                    loadAvatar(UserProfile.getInstance().getUsername());
                    javafx.application.Platform.runLater(() -> {
                        loadAvatar(UserProfile.getInstance().getUsername());
                        GeneralController gc = GeneralController.getInstance();
                        if (gc != null) gc.loadMyAvatar();
                    });
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private java.net.http.HttpRequest.BodyPublisher buildMultipartBody(File file, String boundary) throws Exception {
        byte[] fileBytes = java.nio.file.Files.readAllBytes(file.toPath());
        String header = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n" +
                "Content-Type: image/png\r\n\r\n";
        String footer = "\r\n--" + boundary + "--\r\n";

        byte[] headerBytes = header.getBytes();
        byte[] footerBytes = footer.getBytes();
        byte[] body = new byte[headerBytes.length + fileBytes.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, body, 0, headerBytes.length);
        System.arraycopy(fileBytes, 0, body, headerBytes.length, fileBytes.length);
        System.arraycopy(footerBytes, 0, body, headerBytes.length + fileBytes.length, footerBytes.length);

        return java.net.http.HttpRequest.BodyPublishers.ofByteArray(body);
    }

    @FXML
    private void save() {
        UserProfile profile = UserProfile.getInstance();
        profile.setBio(bioField.getText());
        profile.setStatus(statusCombo.getValue());
        goBack();
    }

    @FXML
    private void logout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Выход из аккаунта");
        alert.setHeaderText(null);
        alert.setContentText("Действительно выйти из аккаунта?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                ApiClient.setToken(null);
                SceneManager.setScene("/fxml/LoginView.fxml", "/css/style.css");
            }
        });
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