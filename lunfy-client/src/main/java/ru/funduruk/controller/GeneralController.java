package ru.funduruk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import lombok.Getter;
import ru.funduruk.manager.SceneManager;
import ru.funduruk.model.GroupDM;
import ru.funduruk.model.MessageStore;
import ru.funduruk.model.UserProfile;
import ru.funduruk.net.ApiClient;
import ru.funduruk.net.WSClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.funduruk.manager.TitleBarManager.maximizeWithoutTaskbar;

public class GeneralController {

    @FXML private BorderPane generalRoot;
    @FXML private HBox titleBar;
    @FXML private VBox groupsPane;
    @FXML private VBox chatListPane;
    @FXML private StackPane contentPane;
    @FXML private javafx.scene.control.Button friendsBtn;
    @FXML private ImageView profileAvatar;
    @FXML private Label profileUsername;
    @FXML private Label profileStatus;

    @Getter
    private static GeneralController instance;

    private double xOffset, yOffset;

    private ChatTabController chatController;


    @FXML
    public void initialize() throws Exception {
        enableWindowDragging();
        enableWindowResize();
        instance = this;
        UserProfile profile = UserProfile.getInstance();
        profileUsername.setText(profile.getUsername() + "#" + profile.getTag());
        profileStatus.setText(profile.getStatus());

        loadChatView();
        loadGroupsView();
        loadChatsFromFriends();

        WSClient.connect("ws://localhost:8080/ws?username=" + ApiClient.getCurrentUsername()
                + "&token=" + ApiClient.getToken());
    }

    private Parent chatView;

    private void loadChatsFromFriends() {
        new Thread(() -> {
            try {
                String response = ApiClient.getRaw("/api/friends");
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, Object>> friends = mapper.readValue(response,
                        new com.fasterxml.jackson.core.type.TypeReference<>() {});

                Platform.runLater(() -> {
                    for (Map<String, Object> f : friends) {
                        String friendUsername = (String) f.get("username");
                        String chatId = buildDmChatId(
                                ApiClient.getCurrentUsername(), friendUsername
                        );
                        addChatIfAbsent(chatId, friendUsername);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String buildDmChatId(String user1, String user2) {
        String[] users = {user1.toLowerCase(), user2.toLowerCase()};
        java.util.Arrays.sort(users);
        return "dm-" + users[0] + "-" + users[1];
    }

    private void loadChatView() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/ChatView.fxml")
            );
            chatView = loader.load();
            chatController = loader.getController();
            contentPane.getChildren().setAll(chatView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addChat(String chatId, String title) {
        Label chat = new Label(title);
        chat.setUserData(chatId);
        chat.getStyleClass().add("chat-item");
        chat.setOnMouseClicked(e -> openChat(chatId));
        chatListPane.getChildren().add(chat);
        MessageStore.getInstance().ensureChat(chatId);
    }

    @FXML private void openFriends() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/FriendsView.fxml")
            );
            Parent view = loader.load();
            contentPane.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML private void close() {
        ((Stage) generalRoot.getScene().getWindow()).close();
    }

    @FXML private void minimize() {
        ((Stage) generalRoot.getScene().getWindow()).setIconified(true);
    }

    @FXML private void maximize() {
        Stage stage = (Stage) generalRoot.getScene().getWindow();
        maximizeWithoutTaskbar(stage);
    }

    private void enableWindowDragging() {
        titleBar.setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
        });
        titleBar.setOnMouseDragged(e -> {
            Stage stage = (Stage) titleBar.getScene().getWindow();
            stage.setX(e.getScreenX() - xOffset);
            stage.setY(e.getScreenY() - yOffset);
        });
    }

    private void enableWindowResize() {
        final int RESIZE_MARGIN = 10;
        generalRoot.setOnMouseMoved(e -> {
            if (e.getX() > generalRoot.getWidth() - RESIZE_MARGIN &&
                    e.getY() > generalRoot.getHeight() - RESIZE_MARGIN) {
                generalRoot.setCursor(javafx.scene.Cursor.SE_RESIZE);
            } else {
                generalRoot.setCursor(javafx.scene.Cursor.DEFAULT);
            }
        });
        generalRoot.setOnMouseDragged(e -> {
            if (generalRoot.getCursor() == javafx.scene.Cursor.SE_RESIZE) {
                Stage stage = (Stage) generalRoot.getScene().getWindow();
                stage.setWidth(e.getX());
                stage.setHeight(e.getY());
            }
        });
    }

    public void openProfile() {
        SceneManager.setScene("/fxml/ProfileView.fxml", "/css/style.css");
    }

    public void setContent(Parent view) {
        contentPane.getChildren().setAll(view);
    }

    private void loadGroupsView() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/GroupsTab.fxml")
            );
            Parent view = loader.load();
            GroupsTabController ctrl = loader.getController();
            ctrl.setGeneralController(this);
            groupsPane.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addChatIfAbsent(String chatId, String title) {
        boolean exists = chatListPane.getChildren().stream()
                .anyMatch(n -> chatId.equals(n.getUserData()));

        if (!exists) {
            Label chat = new Label(title);
            chat.setUserData(chatId);
            chat.getStyleClass().add("chat-item");
            chat.setOnMouseClicked(e -> openChat(chatId));
            chatListPane.getChildren().add(chat);
            MessageStore.getInstance().ensureChat(chatId);
        }
    }

    public void openChat(String chatId) {
        contentPane.getChildren().setAll(chatView);
        chatController.setChatInfo(chatId.replace("dm-", ""), null);

        new Thread(() -> {
            try {
                System.out.println("Loading history for: " + chatId);
                String response = ApiClient.getRaw("/api/chats/" + chatId + "/messages");
                System.out.println("History response: " + response);

                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, Object>> messages = mapper.readValue(response,
                        new com.fasterxml.jackson.core.type.TypeReference<>() {});

                Platform.runLater(() -> {
                    // Очищаем кэш перед загрузкой
                    MessageStore.getInstance().clearChat(chatId);
                    MessageStore.getInstance().ensureChat(chatId);

                    for (Map<String, Object> m : messages) {
                        org.funduruk.dto.MessageDTO msg = new org.funduruk.dto.MessageDTO();
                        msg.setChatId(chatId);
                        msg.setSender((String) m.get("sender"));
                        msg.setText((String) m.get("text"));
                        msg.setMine(ApiClient.getCurrentUsername().equals(msg.getSender()));

                        String ts = (String) m.get("timestamp");
                        try {
                            msg.setTimestamp(java.time.LocalDateTime.parse(ts)
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toInstant().toEpochMilli());
                        } catch (Exception ignored) {}

                        MessageStore.getInstance().addMessage(chatId, msg);
                    }
                    // Открываем чат после загрузки истории
                    chatController.openChat(chatId);
                });
            } catch (Exception e) {
                e.printStackTrace();
                // Если сервер недоступен — просто открываем из кэша
                Platform.runLater(() -> chatController.openChat(chatId));
            }
        }).start();
    }

    public void removeChat(String chatId) {
        chatListPane.getChildren().removeIf(n -> chatId.equals(n.getUserData()));
    }

    private final Map<String, Parent> groupDMViews = new HashMap<>();
    private final Map<String, GroupDMController> groupDMControllers = new HashMap<>();
    @FXML private Button newGroupDMBtn;

    @FXML
    private void openNewGroupDM() {
        javafx.stage.Popup popup = new javafx.stage.Popup();
        popup.setAutoHide(true);

        VBox content = new VBox(8);
        content.setStyle("""
    -fx-background-color: #2d2b40;
    -fx-padding: 12;
    -fx-background-radius: 10;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 4);
""");
        content.setPrefWidth(240);
        content.setMaxWidth(240);

        Label title = new Label("Новый групповой чат");
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");

        TextField nameField = new TextField();
        nameField.setPromptText("Название группы");
        nameField.setStyle("-fx-background-color: #1e1b2e; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6;");

        Label membersLabel = new Label("Выбери друзей:");
        membersLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11px;");

        List<String> friends = List.of("Alice", "Bob", "Charlie");
        List<String> selectedFriends = new ArrayList<>();

        VBox friendCheckboxes = new VBox(4);

        for (String friend : friends) {
            javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(friend);
            cb.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
            cb.setPrefWidth(216);
            cb.setMaxWidth(216);

            cb.setOnAction(e -> {
                if (cb.isSelected()) selectedFriends.add(friend);
                else selectedFriends.remove(friend);
            });
            friendCheckboxes.getChildren().add(cb);
        }

        Button createBtn = new Button("Создать");
        createBtn.setStyle("""
        -fx-background-color: #5865f2;
        -fx-text-fill: white;
        -fx-background-radius: 6;
        -fx-padding: 6 16;
        -fx-cursor: hand;
    """);
        createBtn.setOnAction(e -> {
            if (nameField.getText().isBlank() || selectedFriends.isEmpty()) return;
            createGroupDM(nameField.getText(), selectedFriends);
            popup.hide();
        });

        ScrollPane scrollPane = new ScrollPane(friendCheckboxes);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(120);
        scrollPane.setMaxHeight(120);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        content.getChildren().addAll(title, nameField, membersLabel, scrollPane, createBtn);

        popup.getContent().add(content);



        var bounds = newGroupDMBtn.localToScreen(newGroupDMBtn.getBoundsInLocal());
        popup.show(newGroupDMBtn.getScene().getWindow(), bounds.getMinX() + 30, bounds.getMaxY() + 4);
    }

    private void createGroupDM(String name, List<String> members) {
        String chatId = "gdm-" + System.currentTimeMillis();
        GroupDM gdm = new GroupDM(chatId, name, new ArrayList<>(members));
        gdm.getMembers().add(ApiClient.getCurrentUsername());

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/GroupDMView.fxml")
            );
            Parent view = loader.load();
            GroupDMController ctrl = loader.getController();
            ctrl.setGroupDM(gdm);
            ctrl.setGeneralController(this);

            addChatIfAbsent(chatId, name);

            groupDMViews.put(chatId, view);
            groupDMControllers.put(chatId, ctrl);

            contentPane.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}