package ru.funduruk.controller;

import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import ru.funduruk.manager.SceneManager;
import ru.funduruk.manager.TitleBarManager;

import java.util.Objects;

import static ru.funduruk.manager.TitleBarManager.maximizeWithoutTaskbar;


public class LoginController {


    @FXML
    public BorderPane rootPane;

    @FXML
    public ImageView backgroundLogin;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label statusLabel;

    @FXML
    VBox statusBox;

    @FXML
    private ImageView logo;

    @FXML
    private HBox mainBox;

    @FXML
    private VBox loginWrapper;

    Rectangle2D screenBounds = Screen.getPrimary().getBounds();
    double width = screenBounds.getWidth();

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if(username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Username and password required");
            statusBox.setVisible(true);
            return;
        }

        if(username.equals("test") && password.equals("1234")) {
            playLoginSuccessAnimation();

        } else {
            statusLabel.setText("Invalid credentials");
            statusBox.setVisible(true);
        }
    }

    @FXML
    private void handleRegister() {
        statusLabel.setText("Register clicked!");
    }

    @FXML private HBox titleBar;


    @FXML
    public void initialize() {
        TitleBarManager.enableWindowDragging(titleBar);
        TitleBarManager.enableWindowResize(rootPane);

        String bgPath;

        if(width <= 1280) bgPath = "/image/background/small-lunfy-background.png";
        else if(width <= 1920) bgPath = "/image/background/medium-lunfy-background.png";
        else if(width <= 2560) bgPath = "/image/background/large-lunfy-background.png";
        else bgPath = "/image/background/max-lunfy-background.png";

        Image bgImage = new Image(Objects.requireNonNull(getClass().getResource(bgPath)).toExternalForm());
        backgroundLogin.setImage(bgImage);
        backgroundLogin.fitWidthProperty().bind(rootPane.widthProperty());
        backgroundLogin.fitHeightProperty().bind(rootPane.heightProperty());
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


}
