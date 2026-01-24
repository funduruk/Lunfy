package ru.funduruk.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import static ru.funduruk.manager.ButtonManager.setGlowButton;
import static ru.funduruk.manager.TitleBarManager.*;

public class GeneralController extends Controller{


    @FXML
    public BorderPane generalPane;
    @FXML
    public StackPane generalRoot;
    public VBox groupBox;
    public VBox chatBox;
    public HBox rootContent;
    public BorderPane interfaceBox;
    public Button mainBtn;
    public Button friendsBtn;

    @FXML private HBox titleBar;

    @FXML
    public void initialize() {
        super.initialize(rootPane, titleBar);
        setGlowButton(mainBtn, 0.9, 0.8);
        setGlowButton(friendsBtn, 0.95, 0.2);
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

        if (stage.isMaximized()) {
            stage.setMaximized(false);
        } else {
            maximizeWithoutTaskbar(stage);
        }
    }



    public void handleMainScreen(ActionEvent mouseEvent) {
    }

    public void handleFriendsBtn(ActionEvent actionEvent) {
    }
}
