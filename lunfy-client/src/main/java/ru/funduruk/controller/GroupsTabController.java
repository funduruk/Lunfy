package ru.funduruk.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.Setter;
import ru.funduruk.model.ChatChannel;
import ru.funduruk.model.Group;
import ru.funduruk.model.GroupMember;

import java.util.ArrayList;
import java.util.List;

public class GroupsTabController {

    @FXML private VBox groupsPane;

    private final List<Group> groups = new ArrayList<>();
    @Setter
    private GeneralController generalController;

    @FXML
    public void initialize() {

        Group g1 = new Group("g1", "Lunfy Dev");
        g1.getTextChannels().add(new ChatChannel("g1-general", "general", false));
        g1.getTextChannels().add(new ChatChannel("g1-random", "random", false));
        g1.getVoiceChannels().add(new ChatChannel("g1-voice", "Голосовой", true));
        g1.getMembers().add(new GroupMember("funduruk", "ADMIN", true));
        g1.getMembers().add(new GroupMember("Alice", "MEMBER", true));
        g1.getMembers().add(new GroupMember("Bob", "MEMBER", false));
        groups.add(g1);

        renderGroups();
    }

    private void renderGroups() {
        groupsPane.getChildren().clear();

        for (Group group : groups) {
            Button btn = new Button();
            btn.getStyleClass().add("group-btn");

            Label lbl = new Label(group.getName().substring(0, 2).toUpperCase());
            lbl.getStyleClass().add("group-text");
            btn.setGraphic(lbl);

            btn.setOnAction(e -> openGroup(group));
            groupsPane.getChildren().add(btn);
        }

        Button addBtn = new Button("+");
        addBtn.getStyleClass().add("group-add-btn");
        addBtn.setOnAction(e -> openCreateGroupPopup(addBtn));
        groupsPane.getChildren().add(addBtn);
    }

    private void openGroup(Group group) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/GroupView.fxml")
            );
            Parent view = loader.load();
            GroupViewController ctrl = loader.getController();
            ctrl.setGroup(group);

            if (generalController != null) {
                generalController.setContent(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openCreateGroupPopup(Button anchor) {
        javafx.stage.Popup popup = new javafx.stage.Popup();
        popup.setAutoHide(true);

        VBox content = new VBox(8);
        content.setStyle("""
        -fx-background-color: #2d2b40;
        -fx-padding: 14;
        -fx-background-radius: 10;
        -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 4);
    """);
        content.setPrefWidth(240);
        content.setMaxWidth(240);

        Label title = new Label("Создать сообщество");
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");

        javafx.scene.control.TextField nameField = new javafx.scene.control.TextField();
        nameField.setPromptText("Название сообщества");
        nameField.setStyle("""
        -fx-background-color: #1e1b2e;
        -fx-text-fill: white;
        -fx-prompt-text-fill: #888;
        -fx-background-radius: 6;
        -fx-padding: 6 10;
    """);

        Label channelsLabel = new Label("Текстовые каналы (через запятую):");
        channelsLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11px;");

        javafx.scene.control.TextField channelsField = new javafx.scene.control.TextField();
        channelsField.setPromptText("general, random, новости");
        channelsField.setText("general");
        channelsField.setStyle("""
        -fx-background-color: #1e1b2e;
        -fx-text-fill: white;
        -fx-prompt-text-fill: #888;
        -fx-background-radius: 6;
        -fx-padding: 6 10;
    """);

        Label voiceLabel = new Label("Голосовые каналы (через запятую):");
        voiceLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11px;");

        javafx.scene.control.TextField voiceField = new javafx.scene.control.TextField();
        voiceField.setPromptText("Голосовой, Музыка");
        voiceField.setText("Голосовой");
        voiceField.setStyle("""
        -fx-background-color: #1e1b2e;
        -fx-text-fill: white;
        -fx-prompt-text-fill: #888;
        -fx-background-radius: 6;
        -fx-padding: 6 10;
    """);

        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #ff4d4f; -fx-font-size: 11px;");

        Button createBtn = new Button("Создать");
        createBtn.setStyle("""
        -fx-background-color: #5865f2;
        -fx-text-fill: white;
        -fx-background-radius: 6;
        -fx-padding: 6 16;
        -fx-cursor: hand;
        -fx-font-weight: bold;
    """);

        createBtn.setOnAction(ev -> {
            String name = nameField.getText().trim();
            if (name.isBlank()) {
                errorLabel.setText("Введи название сообщества");
                return;
            }

            String groupId = "g-" + System.currentTimeMillis();
            Group group = new Group(groupId, name);

            String[] textChannels = channelsField.getText().split(",");
            for (String ch : textChannels) {
                String chName = ch.trim();
                if (!chName.isBlank()) {
                    group.getTextChannels().add(
                            new ChatChannel(groupId + "-" + chName, chName, false)
                    );
                }
            }

            String[] voiceChannels = voiceField.getText().split(",");
            for (String ch : voiceChannels) {
                String chName = ch.trim();
                if (!chName.isBlank()) {
                    group.getVoiceChannels().add(
                            new ChatChannel(groupId + "-voice-" + chName, chName, true)
                    );
                }
            }

            group.getMembers().add(new GroupMember(
                    ru.funduruk.model.UserProfile.getInstance().getUsername(),
                    "ADMIN", true
            ));

            groups.add(group);
            renderGroups();
            popup.hide();

            openGroup(group);
        });

        content.getChildren().addAll(title, nameField, channelsLabel, channelsField,
                voiceLabel, voiceField, errorLabel, createBtn);
        popup.getContent().add(content);

        var bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        popup.show(anchor.getScene().getWindow(), bounds.getMaxX() + 8, bounds.getMinY());
    }
}