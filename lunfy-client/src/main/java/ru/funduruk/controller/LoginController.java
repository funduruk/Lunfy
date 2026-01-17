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

import java.util.Objects;


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
    double height = screenBounds.getHeight();

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if(username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Username and password required");
            statusBox.setVisible(true);
            return;
        }
        // TODO FIX LOGIN ANIMATION
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

    private double xOffset = 0;
    private double yOffset = 0;



    @FXML
    public void initialize() {
        enableWindowDragging();
        enableWindowResize();

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

    private void enableWindowDragging() {
        titleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        titleBar.setOnMouseDragged(event -> {
            Stage stage = (Stage) titleBar.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }

    private boolean isInResizeZone(MouseEvent event) {
        double mouseX = event.getX();
        double mouseY = event.getY();
        double width = rootPane.getWidth();
        double height = rootPane.getHeight();
        final int RESIZE_MARGIN = 10;

        return mouseX > width - RESIZE_MARGIN || mouseY > height - RESIZE_MARGIN;
    }

    private void enableWindowResize() {
        rootPane.setOnMouseMoved(event -> {
            if (isInResizeZone(event)) {
                rootPane.setCursor(Cursor.SE_RESIZE);
            } else {
                rootPane.setCursor(Cursor.DEFAULT);
            }
        });

        rootPane.setOnMouseDragged(event -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();

            if (rootPane.getCursor() == Cursor.SE_RESIZE) {
                double newWidth = event.getX();
                double newHeight = event.getY();


                if (newWidth < stage.getMinWidth()) newWidth = stage.getMinWidth();
                if (newHeight < stage.getMinHeight()) newHeight = stage.getMinHeight();

                stage.setWidth(newWidth);
                stage.setHeight(newHeight);
            }
        });
    }

    @FXML
    private void close() {
        ((Stage) titleBar.getScene().getWindow()).close();
    }

    @FXML
    private void minimize() {
        ((Stage) titleBar.getScene().getWindow()).setIconified(true);
    }

    @FXML
    private void maximize() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        stage.setMaximized(!stage.isMaximized());
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
            try {
                wait(5000);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
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
