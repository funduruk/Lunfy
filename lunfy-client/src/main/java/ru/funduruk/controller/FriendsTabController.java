package ru.funduruk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import ru.funduruk.model.Friend;
import ru.funduruk.net.ApiClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FriendsTabController {

    @FXML private TextField searchField;


    @FXML
    public void initialize() {
        searchField.textProperty().addListener((obs, old, val) -> renderList());

        loadFriendsFromServer();
    }

    private final List<Friend> allFriends = new ArrayList<>();

    private void loadFriendsFromServer() {
        new Thread(() -> {
            try {
                String response = ApiClient.getRaw("/api/friends");
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, Object>> data = mapper.readValue(response,
                        new com.fasterxml.jackson.core.type.TypeReference<>() {});

                Platform.runLater(() -> {
                    allFriends.clear();
                    for (Map<String, Object> f : data) {
                        allFriends.add(new Friend(
                                (String) f.get("username"),
                                (String) f.get("tag"),
                                (String) f.getOrDefault("status", "OFFLINE"),
                                "ONLINE".equals(f.get("status")),
                                ((Number) f.getOrDefault("id", 0)).longValue()
                        ));
                    }
                    renderList();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML private Button tabAll, tabOnline, tabIncoming;

    private String currentTab = "ALL";

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
        loadIncomingRequests();
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

        new Thread(() -> {
            try {
                Map<String, Object> result = ApiClient.post(
                        "/api/friends/request",
                        Map.of("username", username, "tag", tag)
                );
                Platform.runLater(() -> {
                    if (result.containsKey("error")) {
                        showError((String) result.get("error"));
                    } else {
                        searchField.clear();
                        searchField.setStyle("");
                        loadFriendsFromServer();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Нет соединения с сервером"));
            }
        }).start();
    }

    @FXML private VBox friendsList;

    private void renderList() {
        friendsList.getChildren().clear();
        String query = searchField.getText().trim().toLowerCase();

        List<Friend> filtered = allFriends.stream()
                .filter(f -> {
                    if (currentTab.equals("ONLINE") && !f.isOnline()) return false;
                    return query.isEmpty() || f.getUsername().toLowerCase().contains(query);
                })
                .toList();

        if (filtered.isEmpty()) {
            Label empty = new Label("Empty.. 👀");
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

        VBox info = new VBox(2);
        Label nameLabel = new Label(friend.getUsername() + "#" + friend.getTag());
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label statusLabel = new Label(friend.getStatus());
        statusLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11px;");
        info.getChildren().addAll(nameLabel, statusLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button msgBtn = new Button("💬");
        msgBtn.getStyleClass().add("friend-action-btn");
        msgBtn.setOnAction(e -> openDirectMessage(friend));

        Button removeBtn = new Button("✕");
        removeBtn.getStyleClass().add("friend-remove-btn");
        removeBtn.setOnAction(e -> {
            new Thread(() -> {
                try {
                    ApiClient.delete("/api/friends/" + friend.getFriendshipId());
                    Platform.runLater(() -> {
                        allFriends.remove(friend);
                        renderList();
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        });

        row.getChildren().addAll(avatarStack, info, spacer, msgBtn, removeBtn);
        return row;
    }

    private void openDirectMessage(Friend friend) {
        String chatId = buildDmChatId(ApiClient.getCurrentUsername(), friend.getUsername());
        GeneralController general = GeneralController.getInstance();
        if (general != null) {
            general.addChatIfAbsent(chatId, friend.getUsername());
            general.openChat(chatId);
        }
    }

    private String buildDmChatId(String user1, String user2) {
        String[] users = {user1.toLowerCase(), user2.toLowerCase()};
        java.util.Arrays.sort(users);
        return "dm-" + users[0] + "-" + users[1];
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

    private void loadIncomingRequests() {
        new Thread(() -> {
            try {
                String response = ApiClient.getRaw("/api/friends/incoming");
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, Object>> data = mapper.readValue(response,
                        new com.fasterxml.jackson.core.type.TypeReference<>() {});

                Platform.runLater(() -> {
                    if (data.isEmpty()) return;

                    friendsList.getChildren().clear();

                    Label incomingLabel = new Label("ВХОДЯЩИЕ ЗАЯВКИ");
                    incomingLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 8 4 4 4;");
                    friendsList.getChildren().add(incomingLabel);

                    for (Map<String, Object> req : data) {
                        HBox row = new HBox(8);
                        row.setAlignment(Pos.CENTER_LEFT);
                        row.setStyle("-fx-padding: 4 8;");

                        Label name = new Label(req.get("from") + "#" + req.get("tag"));
                        name.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");

                        Region spacer = new Region();
                        HBox.setHgrow(spacer, Priority.ALWAYS);

                        Long id = ((Number) req.get("id")).longValue();

                        Button acceptBtn = new Button("✓");
                        acceptBtn.setStyle("-fx-background-color: #3ba55d; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
                        acceptBtn.setOnAction(e -> acceptRequest(id));

                        Button declineBtn = new Button("✕");
                        declineBtn.setStyle("-fx-background-color: #ed4245; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
                        declineBtn.setOnAction(e -> declineRequest(id));

                        row.getChildren().addAll(name, spacer, acceptBtn, declineBtn);
                        friendsList.getChildren().add(row);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void acceptRequest(Long id) {
        new Thread(() -> {
            try {
                ApiClient.post("/api/friends/" + id + "/accept", Map.of());
                Platform.runLater(() -> {
                    loadFriendsFromServer();
                    loadIncomingRequests();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void declineRequest(Long id) {
        new Thread(() -> {
            try {
                ApiClient.post("/api/friends/" + id + "/decline", Map.of());
                Platform.runLater(this::loadIncomingRequests);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}