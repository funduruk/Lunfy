package ru.funduruk.controller;

import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginController {


    @FXML
    public BorderPane rootPane;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label statusLabel;

    @FXML
    VBox statusBox;

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
            statusLabel.setText("Login successful!");
            statusBox.setVisible(true);
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



}
