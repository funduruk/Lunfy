package ru.funduruk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import ru.funduruk.net.ApiClient;
import ru.funduruk.net.ChatEventBus;
import ru.funduruk.net.WSClient;

import java.util.List;
import java.util.Map;

public class GroupDMController {

    @FXML private Label groupNameLabel;
    @FXML private Label groupInitial;
    @FXML private TextArea messageField;

    private GroupDM groupDM;

    public void setGroupDM(GroupDM groupDM) {
        this.groupDM = groupDM;

        groupNameLabel.setText(groupDM.getName());
        groupInitial.setText(groupDM.getName().substring(0, 1).toUpperCase());
        updateMembersCount();
        renderMembers();

        MessageStore.getInstance().ensureChat(groupDM.getId());

        ChatEventBus.addMessageListener(msg -> {
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
        msg.setSender(ApiClient.getCurrentUsername());
        msg.setText(text);
        msg.setTimestamp(System.currentTimeMillis());
        msg.setMine(true);

        WSClient.send(new EnvelopeDTO("CHAT_MESSAGE", msg));
        messageField.clear();
    }

    @FXML private Label membersCountLabel;

    @FXML
    private void addMember() {
        new Thread(() -> {
            try {
                String response = ApiClient.getRaw("/api/friends");
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, Object>> friends = mapper.readValue(response,
                        new com.fasterxml.jackson.core.type.TypeReference<>() {});

                Platform.runLater(() -> {
                    javafx.stage.Popup popup = new javafx.stage.Popup();
                    popup.setAutoHide(true);

                    VBox content = new VBox(6);
                    content.setStyle("-fx-background-color: #2d2b40; -fx-padding: 10; -fx-background-radius: 8;");
                    content.setPrefWidth(200);
                    content.setMaxWidth(200);

                    Label title = new Label("Добавить участника");
                    title.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                    content.getChildren().add(title);

                    boolean anyFriend = false;
                    for (Map<String, Object> f : friends) {
                        String username = (String) f.get("username");
                        String tag = (String) f.get("tag");
                        if (groupDM.getMembers().contains(username)) continue;
                        anyFriend = true;

                        Button btn = new Button(username + "#" + tag);
                        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand;");
                        btn.setMaxWidth(Double.MAX_VALUE);
                        btn.setOnAction(e -> {
                            groupDM.addMember(username);
                            updateMembersCount();
                            renderMembers();
                            popup.hide();
                        });
                        content.getChildren().add(btn);
                    }

                    if (!anyFriend) {
                        Label empty = new Label("Все друзья уже в группе");
                        empty.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11px;");
                        content.getChildren().add(empty);
                    }

                    popup.getContent().add(content);
                    var bounds = membersCountLabel.localToScreen(membersCountLabel.getBoundsInLocal());
                    popup.show(membersCountLabel, bounds.getMinX(), bounds.getMaxY() + 4);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML private Button muteBtnGDM;

    private boolean muted = false;

    @FXML
    private void toggleMute() {
        muted = !muted;
        muteBtnGDM.setText(muted ? "🔕" : "🔔");
    }

    @Setter
    private GeneralController generalController;

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

    @FXML private VBox membersList;

    private void renderMembers() {
        membersList.getChildren().clear();
        String currentUser = ApiClient.getCurrentUsername();
        String owner = groupDM.getOwnerUsername();
        boolean isOwner = currentUser.equals(owner);

        for (String member : groupDM.getMembers()) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-padding: 4 8; -fx-background-radius: 6;");

            Circle dot = new Circle(5);
            dot.setStyle("-fx-fill: #3ba55d;");

            Label name = new Label(member);

            // Paint owner
            boolean isMemberOwner = member.equals(owner);
            name.setStyle("-fx-text-fill: " + (isMemberOwner ? "#f0b132" : "white") +
                    "; -fx-font-size: 12px;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            row.getChildren().addAll(dot, name, spacer);

            boolean canKick = false;
            String kickLabel = "✕";

            if (isOwner && !member.equals(currentUser)) {
                canKick = true;
            } else if (!isOwner && member.equals(currentUser)) {
                canKick = true;
                kickLabel = "🚪";
            }

            if (canKick) {
                Button removeBtn = new Button(kickLabel);
                removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #aaa; -fx-cursor: hand;");
                removeBtn.setOnAction(e -> kickFromGroupDM(member));
                row.getChildren().add(removeBtn);
            }

            membersList.getChildren().add(row);
        }
    }

    private void kickFromGroupDM(String memberToKick) {
        String groupId = groupDM.getId().replace("gdm-", "");

        new Thread(() -> {
            try {
                ApiClient.postRaw(
                        "/api/groups/" + groupId + "/members/" + memberToKick + "/kick",
                        "{}");

                Platform.runLater(() -> {
                    if (memberToKick.equals(ApiClient.getCurrentUsername())) {
                        if (generalController != null) {
                            generalController.removeChat(groupDM.getId());
                            generalController.setContent(new javafx.scene.layout.StackPane());
                        }
                    } else {
                        groupDM.removeMember(memberToKick);
                        updateMembersCount();
                        renderMembers();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateMembersCount() {
        int count = groupDM.getMembers().size();
        membersCountLabel.setText(count + " " + (count == 1 ? "участник" : "участника"));
    }

    @FXML private VBox chatList;

    private void addMessage(MessageDTO msg) {
        boolean mine = ApiClient.getCurrentUsername().equals(msg.getSender());

        String time = java.time.Instant.ofEpochMilli(msg.getTimestamp())
                .atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

        HBox metaBox = new HBox(6);
        metaBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(metaBox, Priority.ALWAYS);
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

        HBox messageWrapper = new HBox(messageLabel);
        messageWrapper.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(messageWrapper, Priority.ALWAYS);
        messageWrapper.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        VBox vbox = new VBox(2);
        vbox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(vbox, Priority.ALWAYS);
        vbox.getChildren().addAll(metaBox, messageWrapper);

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

    @FXML private ScrollPane scrollPane;

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