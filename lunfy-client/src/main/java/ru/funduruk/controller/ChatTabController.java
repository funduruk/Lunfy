package ru.funduruk.controller;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.Getter;
import org.funduruk.dto.EnvelopeDTO;
import org.funduruk.dto.MessageDTO;
import org.funduruk.dto.TypingDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.funduruk.manager.NotificationManager;
import ru.funduruk.model.MessageStore;
import ru.funduruk.net.ApiClient;
import ru.funduruk.net.ChatEventBus;
import ru.funduruk.net.WSClient;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ChatTabController {
    private static final Logger log = LoggerFactory.getLogger(ChatTabController.class);
    private final Label typingIndicator = new Label("Friend is typing...");

    public ScrollPane scrollPane;

    @FXML private VBox chatList;
    @FXML private TextArea messageField;
    @FXML private Button videoBtn;

    @Getter
    public static ChatTabController instance;

    private long lastTypingSent = 0;
    @Getter
    private String currentChatId = null;

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

        ChatEventBus.addMessageListener(msg -> {
            Platform.runLater(() -> {
                MessageStore.getInstance().addMessage(msg.getChatId(), msg);

                boolean isOwn = ApiClient.getCurrentUsername().equalsIgnoreCase(msg.getSender());
                boolean isCurrentChat = msg.getChatId().equals(currentChatId);

                if (isCurrentChat) {
                    addMessage(msg);
                }

                if (!isOwn && !isCurrentChat) {
                    if (NotificationManager.isWindowHidden()) {
                        NotificationManager.showSystem(
                                "Сообщение от " + msg.getSender(), msg.getText());
                    } else {
                        GeneralController gc = GeneralController.getInstance();
                        if (gc != null) gc.incrementUnread(msg.getChatId());
                    }
                }
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

        if (!ru.funduruk.media.VideoCall.isCameraAvailable()) {
            videoBtn.setOpacity(0.4);
            Tooltip noCam = new Tooltip("Нет активных камер");
            noCam.setStyle("-fx-background-color: #13112b; -fx-text-fill: white; -fx-font-size: 12px; -fx-background-radius: 6; -fx-padding: 4 8;");
            noCam.setShowDelay(javafx.util.Duration.millis(200));
            Tooltip.install(videoBtn, noCam);
            videoBtn.setOnAction(e -> {});
        }

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
        msg.setMine(true);

        WSClient.send(new EnvelopeDTO("CHAT_MESSAGE", msg));
        messageField.clear();
    }

    private static final String MY_USER_ID = ApiClient.getCurrentUsername();

    private void addMessage(MessageDTO msg) {
        boolean mine = MY_USER_ID.equals(msg.getSender());

        String time = Instant.ofEpochMilli(msg.getTimestamp())
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm"));

        Label senderLabel = new Label(msg.getSender());
        senderLabel.getStyleClass().add(
                mine ? "message-sender-mine" : "message-sender-other"
        );

        Label timeLabel = new Label(time);
        timeLabel.getStyleClass().add(
                mine ? "message-meta-mine" : "message-meta-other"
        );

        HBox metaBox = new HBox(8, senderLabel, timeLabel);
        metaBox.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        Label messageLabel = new Label(msg.getText());
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(320);
        messageLabel.setStyle(
                "-fx-text-fill: " + (mine ? "white" : "#ffffff") +
                        "; -fx-font-size: 14px;"
        );

        // Bubble only text
        VBox bubble = new VBox(messageLabel);
        bubble.setMaxWidth(360);
        bubble.getStyleClass().addAll(
                "message-bubble",
                mine ? "message-bubble-mine" : "message-bubble-other"
        );

        VBox messageContainer = new VBox(2);
        messageContainer.setAlignment(
                mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT
        );
        messageContainer.getChildren().addAll(metaBox, bubble);

        HBox wrapper = new HBox(8);
        wrapper.setPadding(new Insets(4, 10, 4, 10));
        wrapper.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(wrapper, Priority.ALWAYS);

        if (mine) {
            wrapper.setAlignment(Pos.CENTER_RIGHT);
            wrapper.getChildren().add(messageContainer);
        } else {
            wrapper.setAlignment(Pos.CENTER_LEFT);

            StackPane avatar = buildUserAvatar(msg.getSender(), 32);

            VBox leftBlock = new VBox(2);
            leftBlock.getChildren().addAll(metaBox, bubble);

            wrapper.getChildren().addAll(avatar, leftBlock);
        }

        ContextMenu contextMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("🗑 Удалить сообщение");
        deleteItem.setOnAction(e -> deleteMessage(msg, wrapper));

        if (mine || isAdminInCurrentChat()) {
            contextMenu.getItems().add(deleteItem);
        }

        wrapper.setOnContextMenuRequested(e -> {
            if (!contextMenu.getItems().isEmpty()) {
                contextMenu.show(wrapper, e.getScreenX(), e.getScreenY());
            }
        });

        int idx = chatList.getChildren().indexOf(typingIndicator);

        if (idx >= 0)
            chatList.getChildren().add(idx, wrapper);
        else
            chatList.getChildren().add(wrapper);

        playAppearAnimation(wrapper);
        scrollToBottom();
    }

    private StackPane buildUserAvatar(String username, double size) {
        StackPane container = new StackPane();
        container.setPrefSize(size, size);
        container.setMinSize(size, size);
        container.setMaxSize(size, size);

        javafx.scene.shape.Circle bg = new javafx.scene.shape.Circle(size / 2);
        bg.setFill(javafx.scene.paint.Color.web("#5865f2"));

        String initial = username.length() > 0
                ? username.substring(0, 1).toUpperCase()
                : "?";
        Label letter = new Label(initial);
        letter.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");

        container.getChildren().addAll(bg, letter);

        try {
            String url = ApiClient.HTTP_BASE + "/api/users/" + username + "/avatar";
            javafx.scene.image.Image img = new javafx.scene.image.Image(
                    url, size, size, true, true, true);

            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
            iv.setFitWidth(size);
            iv.setFitHeight(size);
            iv.setPreserveRatio(true);

            javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(
                    size / 2, size / 2, size / 2);
            iv.setClip(clip);

            // Successfully loading - change
            img.errorProperty().addListener((obs, oldVal, newVal) -> {
                // Error loading - stay
            });
            img.progressProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() >= 1.0 && !img.isError()) {
                    Platform.runLater(() -> container.getChildren().setAll(iv));
                }
            });
        } catch (Exception ignored) {}

        return container;
    }

    private boolean isAdminInCurrentChat() {
        if (currentChatId == null) return false;
        return GroupsTabController.isCurrentUserAdminInGroup(currentChatId);
    }

    private void deleteMessage(MessageDTO msg, HBox wrapper) {
        new Thread(() -> {
            try {
                // send request on server
                if (msg.getId() != 0) {
                    ApiClient.delete("/api/chats/messages/" + msg.getId());
                }

                // local delete in hash
                MessageStore.getInstance().getMessages(msg.getChatId())
                        .removeIf(m -> m.getId() == msg.getId()
                                && m.getText().equals(msg.getText()));

                Platform.runLater(() -> {
                    chatList.getChildren().remove(wrapper);
                });

                // notify interlocutor
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
        if (currentChatId == null || currentFriendUsername == null) {
            System.out.println("Нет активного чата для звонка");
            return;
        }
        GeneralController gc = GeneralController.getInstance();

        if (gc.isInCall()) {
            gc.endCurrentCall();
        }

        gc.openCall(ctrl ->
                ctrl.initOutgoing(currentFriendUsername, currentChatId, "AUDIO"));
    }

    @FXML
    private void startVideoCall() {
        if (currentChatId == null || currentFriendUsername == null) return;
        if (!ru.funduruk.media.VideoCall.isCameraAvailable()) {
            System.out.println("Нет камеры");
            return;
        }
        GeneralController gc = GeneralController.getInstance();
        if (gc.isInCall()) {
            gc.endCurrentCall();
        }
        gc.openCall(ctrl -> {
            ctrl.initOutgoing(currentFriendUsername, currentChatId, "AUDIO");
            ctrl.setAutoStartVideo(true);
        });
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

        org.funduruk.dto.MessageDTO msg = new org.funduruk.dto.MessageDTO();
        msg.setChatId(chatIdToDelete);
        ru.funduruk.net.WSClient.send(new org.funduruk.dto.EnvelopeDTO("DELETE_CHAT", msg));

        // clear local
        MessageStore.getInstance().clearChat(chatIdToDelete);
        chatList.getChildren().clear();
        GeneralController.getInstance().removeChat(chatIdToDelete);
        currentChatId = null;
        if (chatName != null) chatName.setText("Чат");
        if (chatAvatarInitial != null) chatAvatarInitial.setText("?");
    }
}