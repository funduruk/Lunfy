package ru.funduruk.controller;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import ru.funduruk.manager.FieldsManager;
import ru.funduruk.manager.SceneManager;

import java.util.Objects;

import static ru.funduruk.manager.TitleBarManager.maximizeWithoutTaskbar;

public class RegisterController extends Controller{

    public BorderPane rootPane;
    public ImageView background;
    public TextField usernameField;
    public TextField emailField;
    public PasswordField passwordField;
    @FXML
    private HBox titleBar;

    @FXML
    private StackPane backButton;
    @FXML
    private Circle backBg;
    @FXML
    private SVGPath backIcon;


    Rectangle2D screenBounds = Screen.getPrimary().getBounds();
    double width = screenBounds.getWidth();

    @FXML
    @Override
    public void initialize() {
        super.initialize(rootPane, titleBar);

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

    private Popup popup;

    public void handleRegister(ActionEvent actionEvent) {

        Label errorLabel = new Label("");
        errorLabel.setStyle("""
        -fx-background-color: #ff4d4f;
        -fx-text-fill: white;
        -fx-padding: 6 10;
        -fx-background-radius: 6;
    """);

        popup = new Popup();
        popup.getContent().add(errorLabel);
        popup.setAutoHide(true);

        if(!FieldsManager.checkPassword(passwordField.getText())) {
            errorLabel.setText("The password must be 8 characters long and contain at least one letter and one number.");
            showPopup(passwordField);
        }
        if(!FieldsManager.checkEmail(emailField.getText())) {
            errorLabel.setText("The email must be a valid email address.");
            showPopup(emailField);
        }

    }

    private void showPopup(TextField field) {
        if (popup.isShowing()) return;

        var bounds = field.localToScreen(field.getBoundsInLocal());

        popup.show(
                field,
                bounds.getMinX(),
                bounds.getMaxY() + 5
        );
    }

    private void showPopup(PasswordField field) {
        if (popup.isShowing()) return;

        var bounds = field.localToScreen(field.getBoundsInLocal());

        popup.show(
                field,
                bounds.getMinX(),
                bounds.getMaxY() + 2
        );
    }

    private void goBackToLogin() {
        SceneManager.setScene("/fxml/LoginView.fxml", "/css/style.css");
    }



}
