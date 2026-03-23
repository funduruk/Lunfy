package ru.funduruk.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import ru.funduruk.model.ChatChannel;
import ru.funduruk.model.Group;
import ru.funduruk.model.GroupMember;

public class GroupViewController {

    @FXML private Label groupNameLabel;
    @FXML private VBox textChannelList;
    @FXML private VBox voiceChannelList;
    @FXML private StackPane chatPane;
    @FXML private VBox adminsList;
    @FXML private VBox membersList;

    private Group group;

    public void setGroup(Group group) {
        this.group = group;
        groupNameLabel.setText(group.getName());
        renderChannels();
        renderMembers();

        if (!group.getTextChannels().isEmpty()) {
            openChannel(group.getTextChannels().get(0));
        }
    }

    private void renderChannels() {
        textChannelList.getChildren().clear();
        voiceChannelList.getChildren().clear();

        for (ChatChannel ch : group.getTextChannels()) {
            textChannelList.getChildren().add(buildChannelItem(ch));
        }
        for (ChatChannel ch : group.getVoiceChannels()) {
            voiceChannelList.getChildren().add(buildChannelItem(ch));
        }
    }

    private HBox buildChannelItem(ChatChannel channel) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("channel-item");

        Label icon = new Label(channel.isVoice() ? "🔊" : "#");
        icon.setStyle("-fx-text-fill: #aaa; -fx-font-size: 14px;");

        Label name = new Label(channel.getName());
        name.setStyle("-fx-text-fill: #ccc; -fx-font-size: 13px;");

        row.getChildren().addAll(icon, name);
        row.setOnMouseClicked(e -> openChannel(channel));

        return row;
    }

    private void openChannel(ChatChannel channel) {
        textChannelList.getChildren().forEach(n -> n.getStyleClass().remove("channel-item-active"));
        voiceChannelList.getChildren().forEach(n -> n.getStyleClass().remove("channel-item-active"));

        if (!channel.isVoice()) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/ChatView.fxml")
                );
                Parent view = loader.load();
                ChatTabController ctrl = loader.getController();
                ctrl.addChat(channel.getId(), channel.getName());
                ctrl.openChat(channel.getId());
                chatPane.getChildren().setAll(view);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // TODO: голосовой канал
            Label placeholder = new Label("🔊 Голосовой канал — скоро!");
            placeholder.setStyle("-fx-text-fill: #aaa; -fx-font-size: 16px;");
            chatPane.getChildren().setAll(placeholder);
        }
    }

    private void renderMembers() {
        adminsList.getChildren().clear();
        membersList.getChildren().clear();

        for (GroupMember member : group.getMembers()) {
            HBox row = buildMemberItem(member);
            if (member.isAdmin()) adminsList.getChildren().add(row);
            else membersList.getChildren().add(row);
        }
    }

    private HBox buildMemberItem(GroupMember member) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("member-item");

        Circle dot = new Circle(5);
        dot.setStyle(member.isOnline() ? "-fx-fill: #3ba55d;" : "-fx-fill: #747f8d;");

        Label name = new Label(member.getUsername());
        name.setStyle("-fx-text-fill: " + (member.isAdmin() ? "#f0b132;" : "#ccc;") +
                " -fx-font-size: 12px;");

        row.getChildren().addAll(dot, name);
        return row;
    }

    @FXML private VBox channelPanel;
    @FXML private VBox membersPanel;
    @FXML private Button hideChannelsBtn;
    @FXML private Button hideMembersBtn;

    private boolean channelPanelVisible = true;
    private boolean membersPanelVisible = true;

    @FXML
    private void toggleChannelPanel() {
        channelPanelVisible = !channelPanelVisible;

        if (channelPanelVisible) {
            channelPanel.setPrefWidth(200);
            channelPanel.setMinWidth(200);
            channelPanel.getChildren().forEach(n -> n.setVisible(true));
            hideChannelsBtn.setText("‹");
        } else {
            channelPanel.setPrefWidth(64);
            channelPanel.setMinWidth(64);
            channelPanel.getChildren().forEach(n -> {
                if (n != channelPanel.getChildren().get(0)) n.setVisible(false);
            });
            hideChannelsBtn.setText("›");
        }
    }

    @FXML
    private void toggleMembersPanel() {
        membersPanelVisible = !membersPanelVisible;

        if (membersPanelVisible) {
            membersPanel.setPrefWidth(200);
            membersPanel.setMinWidth(200);
            membersPanel.getChildren().forEach(n -> n.setVisible(true));
            hideMembersBtn.setText("›");
        } else {
            membersPanel.setPrefWidth(32);
            membersPanel.setMinWidth(32);
            membersPanel.getChildren().forEach(n -> {
                if (n != membersPanel.getChildren().get(0)) n.setVisible(false);
            });
            hideMembersBtn.setText("‹");
        }
    }
}