package ru.funduruk.controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import ru.funduruk.model.Friend;

import java.util.ArrayList;
import java.util.List;

public class FriendsTabController {

    @FXML private VBox friendsList;
    @FXML private TextField searchField;
    @FXML private Button tabAll, tabOnline, tabIncoming;

    private final List<Friend> allFriends = new ArrayList<>();
    private String currentTab = "ALL";

    @FXML
    public void initialize() {
        allFriends.add(new Friend("Alice", "1234", "Онлайн", true));
        allFriends.add(new Friend("Bob", "5678", "Офлайн", false));
        allFriends.add(new Friend("Charlie", "9999", "Не беспокоить", true));

        searchField.textProperty().addListener((obs, old, val) -> renderList());
        renderList();
    }

    @FXML private void showAll() {
        currentTab = "ALL";
        updateTabStyles(tabAll);
        renderList();
    }

    @FXML private void showOnline() {
        currentTab = "ONLINE";
        updateTabStyles(tabOnline);
        renderList();
    }

    @FXML private void showIncoming() {
        currentTab = "INCOMING";
        updateTabStyles(tabIncoming);
        renderList();
    }

    @FXML private void handleAdd() {
        String input = searchField.getText().trim();
        if (!input.contains("#")) {
            showError("Введи имя в формате Username#1234");
            return;
        }
        String[] parts = input.split("#");
        String username = parts[0];
        String tag = parts[1];

        // TODO: отправить запрос на сервер
        System.out.println("Добавляем друга: " + username + "#" + tag);
        searchField.clear();
    }

    private void renderList() {
        friendsList.getChildren().clear();
        String query = searchField.getText().trim().toLowerCase();

        List<Friend> filtered = allFriends.stream()
                .filter(f -> {
                    if (currentTab.equals("ONLINE") && !f.isOnline()) return false;
                    if (!query.isEmpty() && !f.getUsername().toLowerCase().contains(query)) return false;
                    return true;
                })
                .toList();

        if (filtered.isEmpty()) {
            Label empty = new Label("Никого нет 👀");
            empty.setStyle("-fx-text-fill: #aaa; -fx-padding: 20; -fx-font-size: 13px;");
            friendsList.getChildren().add(empty);
            return;
        }

        for (Friend friend : filtered) {
            friendsList.getChildren().add(buildFriendItem(friend));
        }
    }

    private HBox buildFriendItem(Friend friend) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("friend-item");

        StackPane avatarStack = new StackPane();
        Circle avatar = new Circle(20);
        avatar.setStyle("-fx-fill: #5a5480;");

        Label initials = new Label(friend.getUsername().substring(0, 1).toUpperCase());
        initials.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        Circle statusDot = new Circle(6);
        statusDot.setStyle(friend.isOnline()
                ? "-fx-fill: #3ba55d;"
                : "-fx-fill: #747f8d;");
        StackPane.setAlignment(statusDot, Pos.BOTTOM_RIGHT);
        statusDot.setTranslateX(6);
        statusDot.setTranslateY(6);

        avatarStack.getChildren().addAll(avatar, initials, statusDot);

        // Имя и статус
        VBox info = new VBox(2);
        Label nameLabel = new Label(friend.getUsername() + "#" + friend.getTag());
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label statusLabel = new Label(friend.getStatus());
        statusLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11px;");
        info.getChildren().addAll(nameLabel, statusLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Кнопки
        Button msgBtn = new Button("💬");
        msgBtn.getStyleClass().add("friend-action-btn");
        msgBtn.setOnAction(e -> System.out.println("Открыть чат с " + friend.getUsername()));

        Button removeBtn = new Button("✕");
        removeBtn.getStyleClass().add("friend-remove-btn");
        removeBtn.setOnAction(e -> {
            allFriends.remove(friend);
            renderList();
        });

        row.getChildren().addAll(avatarStack, info, spacer, msgBtn, removeBtn);
        return row;
    }

    private void updateTabStyles(Button active) {
        for (Button tab : List.of(tabAll, tabOnline, tabIncoming)) {
            tab.getStyleClass().remove("friends-tab-active");
        }
        active.getStyleClass().add("friends-tab-active");
    }

    private void showError(String msg) {
        searchField.setStyle("-fx-border-color: #ff4d4f;");
        searchField.setPromptText(msg);
    }
}