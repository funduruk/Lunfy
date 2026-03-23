package ru.funduruk.controller;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import lombok.Setter;
import org.funduruk.dto.EnvelopeDTO;
import org.funduruk.dto.MessageDTO;
import ru.funduruk.model.GroupDM;
import ru.funduruk.model.MessageStore;
import ru.funduruk.net.ChatEventBus;
import ru.funduruk.net.WSClient;

public class GroupDMController {

    @FXML private Label groupNameLabel;
    @FXML private Label groupInitial;
    @FXML private Label membersCountLabel;
    @FXML private VBox chatList;
    @FXML private VBox membersList;
    @FXML private ScrollPane scrollPane;
    @FXML private TextArea messageField;
    @FXML private Button muteBtnGDM;

    private GroupDM groupDM;
    @Setter
    private GeneralController generalController;
    private boolean muted = false;

    public void setGroupDM(GroupDM groupDM) {
        this.groupDM = groupDM;

        groupNameLabel.setText(groupDM.getName());
        groupInitial.setText(groupDM.getName().substring(0, 1).toUpperCase());
        updateMembersCount();
        renderMembers();

        MessageStore.getInstance().ensureChat(groupDM.getId());

        ChatEventBus.setOnMessage(msg -> {
            if (groupDM.getId().equals(msg.getChatId())) {
                Platform.runLater(() -> {
                    MessageStore.getInstance().addMessage(msg.getChatId(), msg);
                    addMessage(msg);
                });
            }
        });

        messageField.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER && !event.isShiftDown()) {
                sendMessage();
                event.consume();
            }
        });
    }

    @FXML
    private void sendMessage() {
        String text = messageField.getText();
        if (text.isBlank()) return;

        MessageDTO msg = new MessageDTO();
        msg.setChatId(groupDM.getId());
        msg.setSender("user-1");
        msg.setText(text);
        msg.setTimestamp(System.currentTimeMillis());
        msg.setMine(true);

        WSClient.send(new EnvelopeDTO("CHAT_MESSAGE", msg));
        messageField.clear();
    }

    @FXML
    private void addMember() {

        javafx.stage.Popup popup = new javafx.stage.Popup();
        popup.setAutoHide(true);

        VBox content = new VBox(6);
        content.setStyle("-fx-background-color: #2d2b40; -fx-padding: 10; -fx-background-radius: 8;");

        Label title = new Label("Добавить участника");
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        content.getChildren().add(title);

        for (String friend : java.util.List.of("Alice", "Bob", "Charlie")) {
            if (groupDM.getMembers().contains(friend)) continue;
            Button btn = new Button(friend);
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand;");
            btn.setOnAction(e -> {
                groupDM.addMember(friend);
                updateMembersCount();
                renderMembers();
                popup.hide();
            });
            content.getChildren().add(btn);
        }

        popup.getContent().add(content);
        var bounds = membersCountLabel.localToScreen(membersCountLabel.getBoundsInLocal());
        popup.show(membersCountLabel, bounds.getMinX(), bounds.getMaxY() + 4);
    }

    @FXML
    private void toggleMute() {
        muted = !muted;
        muteBtnGDM.setText(muted ? "🔕" : "🔔");
    }

    @FXML
    private void openSettings() {
        ContextMenu menu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("🗑 Удалить чат");
        deleteItem.setOnAction(e -> {
            MessageStore.getInstance().clearChat(groupDM.getId());
            generalController.removeChat(groupDM.getId());
        });
        menu.getItems().add(deleteItem);
        menu.show(muteBtnGDM, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private void renderMembers() {
        membersList.getChildren().clear();
        for (String member : groupDM.getMembers()) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-padding: 4 8; -fx-background-radius: 6; -fx-cursor: hand;");

            Circle dot = new Circle(5);
            dot.setStyle("-fx-fill: #3ba55d;");

            Label name = new Label(member);
            name.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            if (!member.equals("user-1")) {
                Button removeBtn = new Button("✕");
                removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #aaa; -fx-cursor: hand;");
                removeBtn.setOnAction(e -> {
                    groupDM.removeMember(member);
                    updateMembersCount();
                    renderMembers();
                });
                row.getChildren().addAll(dot, name, spacer, removeBtn);
            } else {
                row.getChildren().addAll(dot, name);
            }

            membersList.getChildren().add(row);
        }
    }

    private void updateMembersCount() {
        int count = groupDM.getMembers().size();
        membersCountLabel.setText(count + " " + (count == 1 ? "участник" : "участника"));
    }

    private void addMessage(MessageDTO msg) {
        boolean mine = "user-1".equals(msg.getSender());

        String time = java.time.Instant.ofEpochMilli(msg.getTimestamp())
                .atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

        HBox metaBox = new HBox(6);
        Label senderLabel = new Label(msg.getSender());
        senderLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10px;");
        Label timeLabel = new Label(time);
        timeLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 10px;");
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
        wrapper.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        wrapper.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        metaBox.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        wrapper.getChildren().add(vbox);

        chatList.getChildren().add(wrapper);
        scrollToBottom();
    }

    private void scrollToBottom() {
        Platform.runLater(() -> {
            final double start = scrollPane.getVvalue();
            final long duration = 200;
            long startTime = System.currentTimeMillis();

            AnimationTimer timer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    double elapsed = System.currentTimeMillis() - startTime;
                    double progress = Math.min(elapsed / duration, 1.0);
                    scrollPane.setVvalue(start + (1.0 - start) * progress);
                    if (progress >= 1.0) stop();
                }
            };
            timer.start();
        });
    }
}