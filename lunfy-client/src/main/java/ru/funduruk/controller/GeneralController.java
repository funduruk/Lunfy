package ru.funduruk.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import ru.funduruk.model.Message;

import java.io.IOException;
import java.util.ArrayList;

import static ru.funduruk.manager.TitleBarManager.maximizeWithoutTaskbar;

public class GeneralController {

    @FXML private BorderPane generalRoot;
    @FXML private HBox titleBar;
    @FXML private VBox groupsPane;
    @FXML private VBox chatListPane;
    @FXML private StackPane contentPane;
    @FXML private javafx.scene.control.Button friendsBtn;

    private double xOffset, yOffset;

    private ChatTabController chatController;


    @FXML
    public void initialize() {
        enableWindowDragging();
        enableWindowResize();

        loadChatTab();
    }

    private void loadChatTab() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ChatView.fxml"));
            Parent chatView = loader.load();
            chatController = loader.getController();
            contentPane.getChildren().setAll(chatView);

            // === Тестовые данные ===
            addChat("chat-1", "Alice");
            addChat("chat-2", "Bob");
            addChat("chat-3", "Charlie");

            chatController.addMessageToChat("chat-1", "Alice", "Привет! Как дела?");
            chatController.addMessageToChat("chat-1", "Me", "Привет, отлично!");
            chatController.addMessageToChat("chat-2", "Bob", "Тестовое сообщение в чате Боба");

            chatController.openChat("chat-1");

            for (int i = 1; i <= 5; i++) {
                javafx.scene.control.Label group = new javafx.scene.control.Label("G" + i);
                group.setStyle("-fx-background-color: #555; -fx-text-fill: white; -fx-padding: 6; -fx-alignment: center; -fx-background-radius: 8;");
                groupsPane.getChildren().add(group);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addChat(String chatId, String title) {
        Label chat = new Label(title);
        chat.getStyleClass().add("chat-item");
        chat.setOnMouseClicked(e -> chatController.openChat(chatId));
        chatListPane.getChildren().add(chat);

        chatController.chatsMessages.putIfAbsent(chatId, new ArrayList<>());
    }

    @FXML private void openFriends() {
        System.out.println("Open friends...");

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
}