package ru.funduruk.controller;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import ru.funduruk.manager.FieldsManager;
import ru.funduruk.manager.SceneManager;
import ru.funduruk.model.UserProfile;
import ru.funduruk.net.ApiClient;

import java.util.Map;
import java.util.Objects;

import static ru.funduruk.manager.TitleBarManager.maximizeWithoutTaskbar;

public class RegisterController extends Controller{

    @FXML private HBox titleBar;
    @FXML Label statusLabel;
    @FXML private StackPane backButton;
    @FXML private Circle backBg;


    Rectangle2D screenBounds = Screen.getPrimary().getBounds();
    double width = screenBounds.getWidth();
    public BorderPane rootPane;

    @FXML
    @Override
    public void initialize() {
        super.initialize(rootPane, titleBar);
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(220);
        String bgPath;

        if(width <= 1280) bgPath = "/image/background/small-lunfy-background.png";
        else if(width <= 1920) bgPath = "/image/background/medium-lunfy-background.png";
        else if(width <= 2560) bgPath = "/image/background/large-lunfy-background.png";
        else bgPath = "/image/background/max-lunfy-background.png";

        Image bgImage = new Image(Objects.requireNonNull(getClass().getResource(bgPath)).toExternalForm());
        background.setImage(bgImage);
        background.fitWidthProperty().bind(rootPane.widthProperty());
        background.fitHeightProperty().bind(rootPane.heightProperty());


        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), backBg);
        fadeIn.setToValue(1);

        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(200), backBg);
        scaleIn.setFromX(0.85);
        scaleIn.setFromY(0.85);
        scaleIn.setToX(1);
        scaleIn.setToY(1);

        ParallelTransition show = new ParallelTransition(fadeIn, scaleIn);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), backBg);
        fadeOut.setToValue(0);

        backButton.setOnMouseEntered(e -> show.playFromStart());
        backButton.setOnMouseExited(e -> fadeOut.playFromStart());

        backButton.setOnMouseClicked(e -> goBackToLogin());
    }


    double prevX, prevY, prevW, prevH;

    @FXML
    private void close() {
        ((Stage) titleBar.getScene().getWindow()).close();
    }

    @FXML
    private void minimize() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        stage.setX(prevX);
        stage.setY(prevY);
        stage.setWidth(prevW);
        stage.setHeight(prevH);
    }

    @FXML
    private void maximize() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        prevX = stage.getX();
        prevY = stage.getY();
        prevW = stage.getWidth();
        prevH = stage.getHeight();

        if (stage.isMaximized()) {
            stage.setMaximized(false);
        } else {
            maximizeWithoutTaskbar(stage);
        }
    }

    public PasswordField passwordField;
    public TextField emailField;
    public TextField usernameField;

    public void handleRegister() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (!FieldsManager.checkPassword(password)) {
            showError("Пароль должен содержать минимум 8 символов, заглавную букву, цифру и спецсимвол");
            return;
        }
        if (!FieldsManager.checkEmail(email)) {
            showError("Введите корректный email");
            return;
        }

        new Thread(() -> {
            try {
                Map<String, Object> result = ApiClient.register(username, email, password);

                javafx.application.Platform.runLater(() -> {
                    if (result.containsKey("token")) {
                        UserProfile profile = UserProfile.getInstance();
                        profile.setUsername((String) result.get("username"));
                        profile.setTag((String) result.get("tag"));

                        SceneManager.setScene("/fxml/GeneralView.fxml", "/css/style.css");
                    } else {
                        showError((String) result.getOrDefault("error", "Ошибка регистрации"));
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                        showError("Нет соединения с сервером")
                );
            }
        }).start();
    }

    @FXML private VBox statusBox;

    private void showError(String text) {
        statusLabel.setText(text);
        statusBox.setVisible(true);
    }

    private void goBackToLogin() {
        SceneManager.setScene("/fxml/LoginView.fxml", "/css/style.css");
    }



}
