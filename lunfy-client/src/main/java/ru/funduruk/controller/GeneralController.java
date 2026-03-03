package ru.funduruk.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import ru.funduruk.manager.SceneManager;
import ru.funduruk.model.MessageStore;
import ru.funduruk.model.UserProfile;
import ru.funduruk.net.WSClient;

import java.util.ArrayList;

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

    private double xOffset, yOffset;

    private ChatTabController chatController;


    @FXML
    public void initialize() throws Exception {
        enableWindowDragging();
        enableWindowResize();

        UserProfile profile = UserProfile.getInstance();
        profileUsername.setText(profile.getUsername());
        profileStatus.setText(profile.getStatus());
        if (profile.getAvatarPath() != null) {
            profileAvatar.setImage(new Image("file:" + profile.getAvatarPath()));
        }

        loadChatView();
        loadGroupsView();
        WSClient.connect("ws://localhost:8080/ws");

        chatController.addChat("test-chat", "Test Chat");
        addChat("test-chat", "Test Chat");
        chatController.openChat("test-chat");
        chatController.addMessageToChat("test-chat", "Me", "Hello!");
    }

    private Parent chatView;

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
        chat.getStyleClass().add("chat-item");
        chat.setOnMouseClicked(e -> {
            contentPane.getChildren().setAll(chatView);
            chatController.openChat(chatId);
        });
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
}