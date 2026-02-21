package ru.funduruk.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class FriendsTabController {

    @FXML
    private VBox friendsList;

    @FXML
    public void initialize() {
        // Тестовые друзья
        addFriend("Alice");
        addFriend("Bob");
        addFriend("Charlie");
    }

    private void addFriend(String name) {
        Label friend = new Label(name);
        friend.setStyle("-fx-text-fill: white; -fx-background-color: #221E33; -fx-padding: 5; -fx-background-radius: 6;");
        friendsList.getChildren().add(friend);
    }
}
