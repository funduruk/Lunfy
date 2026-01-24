package ru.funduruk.controller;

import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import ru.funduruk.manager.TitleBarManager;

public abstract class Controller {

    public BorderPane rootPane;
    public ImageView background;
    @FXML
    private HBox titleBar;
    @FXML
    public void initialize( BorderPane rootPane, HBox titleBar ) {
        TitleBarManager.enableWindowDragging(titleBar);
        TitleBarManager.enableWindowResize(rootPane);
    }

    @FXML
    public abstract void initialize();
}
