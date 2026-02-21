package ru.funduruk.controller;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import ru.funduruk.model.dto.MessageDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatTabController {

    public ScrollPane scrollPane;
    @FXML private VBox chatList;
    @FXML private TextArea messageField;

    public final Map<String, List<MessageDTO>> chatsMessages = new HashMap<>();
    private String currentChatId = null;


    private Label typingIndicator = new Label("Friend is typing...");

    public void initialize() {
        typingIndicator.setStyle("-fx-text-fill: #aaa; -fx-font-style: italic; -fx-padding: 2;");
        typingIndicator.setVisible(false);

        messageField.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER -> {
                    if (event.isShiftDown()) {
                        int caret = messageField.getCaretPosition();
                        messageField.insertText(caret, "\n");
                    } else {
                        sendMessage();
                        event.consume();
                    }
                }
            }
        });

    }

    public void addChat(String chatId, String title) {
        Label chat = new Label(title);
        chat.getStyleClass().add("chat-item");
        chat.setOnMouseClicked(e -> openChat(chatId));
        chatList.getChildren().add(chat);
        chatsMessages.putIfAbsent(chatId, new ArrayList<>());
    }

    public void addMessageToChat(String chatId, String sender, String text) {
        MessageDTO msg = new MessageDTO(chatId, sender, text, sender.equals("Me"));
        chatsMessages.get(chatId).add(msg);
        if (chatId.equals(currentChatId)) addMessage(msg);
    }

    public void openChat(String chatId) {
        currentChatId = chatId;
        chatList.getChildren().clear();
        for (MessageDTO msg : chatsMessages.getOrDefault(chatId, new ArrayList<>())) {
            addMessage(msg);
        }

        if (!chatList.getChildren().contains(typingIndicator)) {
            chatList.getChildren().add(typingIndicator);
        }
        typingIndicator.setVisible(false);
    }

    @FXML
    private void sendMessage() {
        String text = messageField.getText();
        if (text.isBlank()) return;

        addMessageToChat(currentChatId, "Me", text);
        messageField.clear();

        typingIndicator.setVisible(true);
        Platform.runLater(() -> {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            addMessageToChat(currentChatId, "Friend", "Привет! Это автоответ 😎");
            typingIndicator.setVisible(false);
        });
    }

    private void addMessage(MessageDTO msg) {
        HBox wrapper = new HBox();
        wrapper.setSpacing(6);
        wrapper.setAlignment(msg.isMine() ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        VBox vbox = new VBox(2);

        HBox userInfo = new HBox(4);
        userInfo.setAlignment(msg.isMine() ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        ImageView avatar = new ImageView(new Image(
                msg.isMine() ? "/image/logo/logo.png" : "/image/logo/logo.png",
                24, 24, true, true
        ));
        Label senderLabel = new Label(msg.getSender());
        senderLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10px;");

        if (msg.isMine()) {
            userInfo.getChildren().addAll(senderLabel, avatar);
        } else {
            userInfo.getChildren().addAll(avatar, senderLabel);
        }

        Label messageLabel = new Label(msg.getText());
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(300);
        messageLabel.getStyleClass().add(msg.isMine() ? "message-out" : "message-in");

        vbox.getChildren().addAll(userInfo, messageLabel);
        wrapper.getChildren().add(vbox);

        int idx = chatList.getChildren().indexOf(typingIndicator);
        if (idx >= 0) chatList.getChildren().add(idx, wrapper);
        else chatList.getChildren().add(wrapper);

        playAppearAnimation(wrapper);

        scrollToBottom();
    }

    private void playAppearAnimation(HBox node) {
        FadeTransition fade = new FadeTransition(Duration.millis(200), node);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(200), node);
        slide.setFromY(10);
        slide.setToY(0);

        fade.play();
        slide.play();
    }

    private void scrollToBottom() {
        Platform.runLater(() -> {
            final double start = scrollPane.getVvalue();
            final double end = 1.0;
            final long duration = 200;

            long startTime = System.currentTimeMillis();

            AnimationTimer timer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    double elapsed = System.currentTimeMillis() - startTime;
                    double progress = Math.min(elapsed / duration, 1.0);

                    scrollPane.setVvalue(start + (end - start) * progress);

                    if (progress >= 1.0) {
                        stop();
                    }
                }
            };
            timer.start();
        });
    }
}