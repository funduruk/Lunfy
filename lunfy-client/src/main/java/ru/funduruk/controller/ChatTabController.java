package ru.funduruk.controller;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.Getter;
import org.funduruk.dto.EnvelopeDTO;
import org.funduruk.dto.MessageDTO;
import org.funduruk.dto.TypingDTO;
import ru.funduruk.model.MessageStore;
import ru.funduruk.net.ChatEventBus;
import ru.funduruk.net.WSClient;

public class ChatTabController {

    public ScrollPane scrollPane;
    @FXML private VBox chatList;
    @FXML private TextArea messageField;

    private String currentChatId = null;


    private Label typingIndicator = new Label("Friend is typing...");

    @Getter
    public static ChatTabController instance;

    private long lastTypingSent = 0;

    @FXML
    public void initialize() {


        System.out.println("ChatTabController INITIALIZED");

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

            System.out.println("UI RECEIVED MESSAGE: " + msg.getText());
            Platform.runLater(() -> {

                addMessageToChat(
                        msg.getChatId(),
                        msg.getSender(),
                        msg.getText()
                );
            });
        });


        messageField.textProperty().addListener((obs, old, val) -> {
            long now = System.currentTimeMillis();

            if (now - lastTypingSent > 500) { // throttle
//                sendTyping(true);
                lastTypingSent = now;
            }
        });
    }

//    private void sendTyping(boolean typing) {
//        TypingDTO dto = new TypingDTO();
//        dto.setChatId(currentChatId);
//        dto.setUserId("user-1");
//
//        WSClient.send("TYPING", dto);
//    }

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
        msg.setSender("user-1");
        msg.setText(text);
        msg.setTimeStamp(System.currentTimeMillis());
        System.out.println("\n\n CURRENT TIME: " + msg.getTimeStamp() + "\n\n");
        msg.setMine(true);

        WSClient.send(new EnvelopeDTO("CHAT_MESSAGE", msg));
        messageField.clear();
    }
    private static final String MY_USER_ID = "user-1";

    private void addMessage(MessageDTO msg) {
        boolean mine = MY_USER_ID.equals(msg.getSender());
        System.out.println("addMessage: sender=" + msg.getSender() + " mine=" + mine + " text=" + msg.getText());
        // Время
        String time = java.time.Instant.ofEpochMilli(msg.getTimeStamp())
                .atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

        // Никнейм + время
        HBox metaBox = new HBox(6);
        Label senderLabel = new Label(msg.getSender());
        senderLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 10px;");
        Label timeLabel = new Label(time);
        timeLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 10px;");
        metaBox.getChildren().addAll(senderLabel, timeLabel);

        // Текст сообщения
        Label messageLabel = new Label(msg.getText());
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(300);
        messageLabel.setStyle(mine
                ? "-fx-background-color: #31638a; -fx-text-fill: white; -fx-padding: 6 10; -fx-background-radius: 12;"
                : "-fx-background-color: #221E33; -fx-text-fill: white; -fx-padding: 6 10; -fx-background-radius: 12;"
        );

        // Контейнер никнейм + сообщение
        VBox vbox = new VBox(2);
        vbox.getChildren().addAll(metaBox, messageLabel);

        // Обёртка для выравнивания
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


    public void onMessage(MessageDTO msg) {
        Platform.runLater(() -> addMessage(msg));
    }

    public void showTyping(String chatId, boolean isTyping) {
        if (!chatId.equals(currentChatId)) return;
        Platform.runLater(() -> typingIndicator.setVisible(isTyping));
    }

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
}