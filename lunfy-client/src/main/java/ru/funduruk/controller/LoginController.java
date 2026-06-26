package ru.funduruk.controller;

import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import ru.funduruk.manager.SceneManager;
import ru.funduruk.model.UserProfile;
import ru.funduruk.net.ApiClient;

import java.util.Map;
import java.util.Objects;

import static ru.funduruk.manager.TitleBarManager.maximizeWithoutTaskbar;


public class LoginController extends Controller{

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML VBox statusBox;
    @FXML private TextField passwordVisibleField;
    @FXML private Button eyeButton;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Введите имя пользователя \n и/или пароль");
            statusBox.setVisible(true);
            return;
        }

        new Thread(() -> {
            try {
                Map<String, Object> result = ApiClient.login(username, password);

                javafx.application.Platform.runLater(() -> {
                    if ("verification_required".equals(result.get("status"))) {
                        VerifyEmailController.setPending(
                                (String) result.get("email"),
                                username,
                                null
                        );
                        SceneManager.setScene("/fxml/VerifyEmailView.fxml", "/css/style.css");
                        return;
                    }

                    if (result.containsKey("token")) {
                        UserProfile profile = UserProfile.getInstance();
                        profile.setUsername((String) result.get("username"));
                        profile.setTag((String) result.get("tag"));
                        playLoginSuccessAnimation();
                    } else {
                        statusLabel.setText((String) result.getOrDefault("error", "Ошибка входа"));
                        statusBox.setVisible(true);
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText("Нет соединения с сервером");
                    statusBox.setVisible(true);
                });
            }
        }).start();
    }

    @FXML
    private void handleRegister() {
        SceneManager.setScene(
                "/fxml/RegisterView.fxml",
                "/css/style.css"
        );
    }

    @FXML private HBox titleBar;
    @FXML public ImageView backgroundLogin;
    @FXML public BorderPane rootPane;

    public Button registerBtn;
    Rectangle2D screenBounds = Screen.getPrimary().getBounds();
    double width = screenBounds.getWidth();

    @FXML
    public void initialize() {
        super.initialize(rootPane, titleBar);
        setupHoverAnimation(registerBtn);

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

    @FXML private VBox loginWrapper;
    @FXML private ImageView logo;

    private void playLoginSuccessAnimation() {

        double loginWidth = loginWrapper.getWidth();


        ScaleTransition scale = new ScaleTransition(Duration.millis(400), loginWrapper);
        scale.setFromX(1);
        scale.setToX(0);
        scale.setFromY(1);
        scale.setToY(1);

        Image gif = new Image(
                Objects.requireNonNull(getClass().getResource("/gif/loading.gif")).toExternalForm()
        );
        logo.setImage(gif);

        ParallelTransition animation = getParallelTransition(loginWidth, scale);


        animation.setOnFinished(e -> {
            SceneManager.setScene(
                    "/fxml/GeneralView.fxml",
                    "/css/style.css"
            );
        });

        animation.play();
    }

    @FXML private HBox mainBox;

    private ParallelTransition getParallelTransition(double loginWidth, ScaleTransition scale) {
        TranslateTransition moveLeft =
                new TranslateTransition(Duration.millis(400), loginWrapper);
        moveLeft.setFromX(0);
        moveLeft.setToX(-loginWidth / 2);

        TranslateTransition logoMove =
                new TranslateTransition(Duration.millis(400), logo);

        logoMove.setToX(-(mainBox.getWidth() / 2 - logo.getFitWidth() / 2));

        return new ParallelTransition(
                scale,
                moveLeft,
                logoMove
        );
    }

    private void setupHoverAnimation(Button button) {

        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(200), button);
        scaleIn.setToX(1.1);
        scaleIn.setToY(1.1);

        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(200), button);
        scaleOut.setToX(1);
        scaleOut.setToY(1);

        button.setOnMouseEntered(e -> {
            button.setStyle(
                    "-fx-background-color: #31638a;" +
                            "-fx-border-color: #31638a;" +
                            "-fx-font-size: 12px;"
            );
            scaleIn.playFromStart();
        });

        button.setOnMouseExited(e -> {
            button.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-border-color: transparent;" +
                            "-fx-font-size: 10px;"
            );
            scaleOut.playFromStart();
        });
    }

    @FXML
    private void handleForgotPassword() {
        SceneManager.setScene("/fxml/ForgotPasswordView.fxml", "/css/style.css");
    }
}
