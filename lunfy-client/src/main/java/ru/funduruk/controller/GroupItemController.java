package ru.funduruk.controller;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class GroupItemController {

    @FXML private StackPane root;
    @FXML private ImageView avatar;

    private boolean selected = false;

    public void setAvatar(Image image) {
        avatar.setImage(image);
    }

    @FXML
    private void initialize() {
        root.setOnMouseClicked(e -> toggle());
    }

    private void toggle() {
        selected = !selected;
        if (selected) {
            root.getStyleClass().add("selected");
        } else {
            root.getStyleClass().remove("selected");
        }
    }
}
