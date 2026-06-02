package ru.funduruk.controller;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.Getter;
import org.funduruk.dto.EnvelopeDTO;
import org.funduruk.dto.MessageDTO;
import org.funduruk.dto.TypingDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.funduruk.model.MessageStore;
import ru.funduruk.net.ApiClient;
import ru.funduruk.net.ChatEventBus;
import ru.funduruk.net.WSClient;

public class ChatTabController {

    private static final Logger log = LoggerFactory.getLogger(ChatTabController.class);
    public ScrollPane scrollPane;
    @FXML private VBox chatList;
    @FXML private TextArea messageField;

    private String currentChatId = null;


    private final Label typingIndicator = new Label("Friend is typing...");

    @Getter
    public static ChatTabController instance;

    private long lastTypingSent = 0;

    @FXML
    public void initialize() {
        log.info("ChatTabController INITIALIZED");

        scrollPane.setFitToWidth(true);
        chatList.setFillWidth(true);
        chatList.prefWidthProperty().bind(scrollPane.widthProperty().subtract(20));


        instance = this;

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

        ChatEventBus.setOnMessage(msg -> {


            log.trace("UI RECEIVED MESSAGE: {}", msg.getText());

            Platform.runLater(() -> {
                MessageStore.getInstance().addMessage(msg.getChatId(), msg);
                if (msg.getChatId().equals(currentChatId)) addMessage(msg);
            });
        });

        ChatEventBus.setOnDeleteMessage(msg -> {
            Platform.runLater(() -> {
                MessageStore.getInstance().getMessages(msg.getChatId())
                        .removeIf(m -> m.getId() == msg.getId());

                if (msg.getChatId().equals(currentChatId)) {
                    chatList.getChildren().clear();
                    for (MessageDTO m : MessageStore.getInstance().getMessages(currentChatId)) {
                        addMessage(m);
                    }
                }
            });
        });

        messageField.textProperty().addListener((obs, old, val) -> {
            long now = System.currentTimeMillis();

            if (now - lastTypingSent > 500) {
                lastTypingSent = now;
            }
        });

        ChatEventBus.setOnDeleteChat(chatId -> {
            Platform.runLater(() -> {
                MessageStore.getInstance().clearChat(chatId);
                if (chatId.equals(currentChatId)) {
                    chatList.getChildren().clear();
                    currentChatId = null;
                    if (chatName != null) chatName.setText("Чат");
                    if (chatAvatarInitial != null) chatAvatarInitial.setText("?");
                }
                GeneralController general = GeneralController.getInstance();
                if (general != null) general.removeChat(chatId);
            });
        });
    }

    public void addChat(String chatId, String title) {
        MessageStore.getInstance().ensureChat(chatId);
        Label chat = new Label(title);
        chat.getStyleClass().add("chat-item");
        chat.setOnMouseClicked(e -> openChat(chatId));
        chatList.getChildren().add(chat);
    }

    public void addMessageToChat(String chatId, String sender, String text) {
        MessageDTO msg = new MessageDTO();
        msg.setChatId(chatId);
        msg.setSender(sender);
        msg.setText(text);
        msg.setMine(false);
        MessageStore.getInstance().addMessage(chatId, msg);
        if (chatId.equals(currentChatId)) addMessage(msg);
    }

    public void openChat(String chatId) {
        currentChatId = chatId;
        chatList.getChildren().clear();
        for (MessageDTO msg : MessageStore.getInstance().getMessages(chatId)) {
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

        MessageDTO msg = new MessageDTO();
        msg.setChatId(currentChatId);
        msg.setSender(ApiClient.getCurrentUsername());
        msg.setText(text);
        msg.setTimestamp(System.currentTimeMillis());
        System.out.println("\n\n CURRENT TIME: " + msg.getTimestamp() + "\n\n");
        msg.setMine(true);

        WSClient.send(new EnvelopeDTO("CHAT_MESSAGE", msg));
        messageField.clear();
    }
    private static final String MY_USER_ID = ApiClient.getCurrentUsername();

    private void addMessage(MessageDTO msg) {
        boolean mine = MY_USER_ID.equals(msg.getSender());
        System.out.println("addMessage: sender=" + msg.getSender() + " mine=" + mine + " text=" + msg.getText());

        String time = java.time.Instant.ofEpochMilli(msg.getTimestamp())
                .atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        System.out.println("TIMESTAMP IN addMessage: " + msg.getTimestamp());

        HBox metaBox = new HBox(6);
        Label senderLabel = new Label(msg.getSender());
        senderLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 10px;");
        Label timeLabel = new Label(time);
        timeLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 10px;");
        metaBox.getChildren().addAll(senderLabel, timeLabel);

        Label messageLabel = new Label(msg.getText());
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(300);
        messageLabel.setStyle(mine
                ? "-fx-background-color: #31638a; -fx-text-fill: white; -fx-padding: 6 10; -fx-background-radius: 12;"
                : "-fx-background-color: #221E33; -fx-text-fill: white; -fx-padding: 6 10; -fx-background-radius: 12;"
        );

        VBox vbox = new VBox(2);
        vbox.getChildren().addAll(metaBox, messageLabel);

        HBox wrapper = new HBox();
        wrapper.setPadding(new javafx.geometry.Insets(4, 10, 4, 10));
        HBox.setHgrow(wrapper, javafx.scene.layout.Priority.ALWAYS);
        wrapper.setMaxWidth(Double.MAX_VALUE);

        if (mine) {
            wrapper.setAlignment(Pos.CENTER_RIGHT);
            metaBox.setAlignment(Pos.CENTER_RIGHT);
        } else {
            wrapper.setAlignment(Pos.CENTER_LEFT);
            metaBox.setAlignment(Pos.CENTER_LEFT);
        }

        wrapper.getChildren().add(vbox);

        ContextMenu contextMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("🗑 Удалить сообщение");
        deleteItem.setStyle("-fx-text-fill: #ed4245;");
        deleteItem.setOnAction(e -> deleteMessage(msg, wrapper));


        boolean canDelete = mine || isAdminInCurrentChat();
        if (canDelete) {
            contextMenu.getItems().add(deleteItem);
        }

        if (!contextMenu.getItems().isEmpty()) {
            wrapper.setOnMouseClicked(e -> {
                if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                    contextMenu.show(wrapper, e.getScreenX(), e.getScreenY());
                }
            });
        }

        wrapper.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                contextMenu.show(wrapper, e.getScreenX(), e.getScreenY());
            }
        });

        int idx = chatList.getChildren().indexOf(typingIndicator);
        if (idx >= 0) chatList.getChildren().add(idx, wrapper);
        else chatList.getChildren().add(wrapper);

        playAppearAnimation(wrapper);
        scrollToBottom();
    }

    private boolean isAdminInCurrentChat() {
        if (currentChatId == null) return false;
        // Для групповых каналов проверяем роль
        // chatId канала это числовой ID — проверяем через GroupsTabController
        return GroupsTabController.isCurrentUserAdminInGroup(currentChatId);
    }

    private void deleteMessage(MessageDTO msg, HBox wrapper) {
        new Thread(() -> {
            try {
                // Отправляем запрос на сервер
                if (msg.getId() != 0) {
                    ApiClient.delete("/api/chats/messages/" + msg.getId());
                }

                // Удаляем локально из кэша
                MessageStore.getInstance().getMessages(msg.getChatId())
                        .removeIf(m -> m.getId() == msg.getId()
                                && m.getText().equals(msg.getText()));

                Platform.runLater(() -> {
                    chatList.getChildren().remove(wrapper);
                });

                // Уведомляем собеседника через WebSocket
                org.funduruk.dto.EnvelopeDTO env = new org.funduruk.dto.EnvelopeDTO(
                        "DELETE_MESSAGE", msg
                );
                ru.funduruk.net.WSClient.send(env);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
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


    public void onMessage(MessageDTO msg) {
        Platform.runLater(() -> addMessage(msg));
    }

    //TODO
    public void showTyping(String chatId, boolean isTyping) {
        if (!chatId.equals(currentChatId)) return;
        Platform.runLater(() -> typingIndicator.setVisible(isTyping));
    }

    //TODO
    public void onTyping(TypingDTO dto) {
        if (!dto.getChatId().equals(currentChatId)) return;

        Platform.runLater(() -> {
            typingIndicator.setText(dto.getUserId() + " печатает...");
            typingIndicator.setVisible(dto.isTyping());

            if (dto.isTyping()) {
                autoHideTyping();
            }
        });
    }

    private void autoHideTyping() {
        PauseTransition delay = new PauseTransition(Duration.seconds(3));
        delay.setOnFinished(e -> typingIndicator.setVisible(false));
        delay.play();
    }

    @FXML private Label chatName;
    @FXML private Label chatStatusLabel;
    @FXML private ImageView chatAvatar;
    @FXML private Label chatAvatarInitial;
    @FXML private Button muteBtn;
    @FXML private Button settingsBtn;

    private boolean muted = false;
    private String currentFriendUsername = null;

    public void setChatInfo(String name, String avatarPath) {
        currentFriendUsername = name;
        chatName.setText(name);
        chatAvatarInitial.setText(name.substring(0, 1).toUpperCase());

        if (avatarPath != null) {
            chatAvatar.setImage(new Image("file:" + avatarPath));
            chatAvatarInitial.setVisible(false);
        } else {
            chatAvatar.setImage(null);
            chatAvatarInitial.setVisible(true);
        }
    }

    @FXML
    private void openFriendProfile() {
        if (currentFriendUsername == null) return;
        System.out.println("Open profile: " + currentFriendUsername);
        // TODO: открыть профиль друга
    }

    @FXML
    private void startCall() {
        System.out.println("Later..!");
    }

    @FXML
    private void startVideoCall() {
        System.out.println("Later..!");
    }

    @FXML
    private void toggleMute() {
        muted = !muted;
        muteBtn.setText(muted ? "🔕" : "🔔");
        log.info("Notify {}", muted ? "off" : "on");
    }

    @FXML
    private void openChatSettings() {
        javafx.scene.control.ContextMenu menu = new javafx.scene.control.ContextMenu();

        javafx.scene.control.MenuItem deleteChat = new javafx.scene.control.MenuItem("🗑 Удалить чат");
        deleteChat.setOnAction(e -> deleteCurrentChat());

        javafx.scene.control.MenuItem muteItem = new javafx.scene.control.MenuItem(
                muted ? "🔔 On notify" : "🔕 Off notify"
        );
        muteItem.setOnAction(e -> toggleMute());

        menu.getItems().addAll(muteItem, deleteChat);
        menu.show(settingsBtn,
                javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private void deleteCurrentChat() {
        if (currentChatId == null) return;

        String chatIdToDelete = currentChatId;

        // Отправляем через WebSocket — сервер сам удалит из БД и уведомит собеседника
        org.funduruk.dto.MessageDTO msg = new org.funduruk.dto.MessageDTO();
        msg.setChatId(chatIdToDelete);
        ru.funduruk.net.WSClient.send(new org.funduruk.dto.EnvelopeDTO("DELETE_CHAT", msg));

        // Очищаем локально
        MessageStore.getInstance().clearChat(chatIdToDelete);
        chatList.getChildren().clear();
        GeneralController.getInstance().removeChat(chatIdToDelete);
        currentChatId = null;
        if (chatName != null) chatName.setText("Чат");
        if (chatAvatarInitial != null) chatAvatarInitial.setText("?");
    }
}