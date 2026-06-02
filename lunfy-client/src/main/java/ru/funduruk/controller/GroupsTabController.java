package ru.funduruk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import ru.funduruk.model.ChatChannel;
import ru.funduruk.model.Group;
import ru.funduruk.model.GroupMember;
import ru.funduruk.net.ApiClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GroupsTabController {

    @FXML
    private VBox groupsPane;

    private final List<Group> groups = new ArrayList<>();
    @Setter
    private GeneralController generalController;

    @Getter
    private static GroupsTabController instance;

    @FXML
    public void initialize() {
        loadGroupsFromServer();
        instance = this;
    }

    private void loadGroupsFromServer() {
        new Thread(() -> {
            try {
                String response = ApiClient.getRaw("/api/groups");
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, Object>> data = mapper.readValue(response,
                        new com.fasterxml.jackson.core.type.TypeReference<>() {
                        });

                Platform.runLater(() -> {
                    groups.clear();
                    for (Map<String, Object> g : data) {
                        String groupId = String.valueOf(g.get("id"));
                        String name = (String) g.get("name");
                        String avatarPath = (String) g.get("avatarPath");

                        Group group = new Group(groupId, name);

                        List<Map<String, Object>> channels =
                                (List<Map<String, Object>>) g.get("channels");

                        for (Map<String, Object> ch : channels) {
                            String chId = String.valueOf(ch.get("id"));
                            String chName = (String) ch.get("name");
                            group.setAvatarPath(avatarPath);
                            boolean isVoice = "VOICE".equals(ch.get("type"));
                            ChatChannel channel = new ChatChannel(chId, chName, isVoice);

                            if (isVoice) group.getVoiceChannels().add(channel);
                            else group.getTextChannels().add(channel);
                        }

                        groups.add(group);
                    }
                    renderGroups();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void renderGroups() {
        groupsPane.getChildren().clear();

        for (Group group : groups) {
            Button btn = new Button();
            btn.getStyleClass().add("group-btn");

            if (group.getAvatarPath() != null && !group.getAvatarPath().isBlank()) {
                // Аватарка группы
                javafx.scene.image.ImageView avatar = new javafx.scene.image.ImageView(
                        new javafx.scene.image.Image("file:" + group.getAvatarPath())
                );
                avatar.setFitWidth(42);
                avatar.setFitHeight(42);
                avatar.setPreserveRatio(true);

                // Обрезаем в круг
                javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(21, 21, 21);
                avatar.setClip(clip);
                btn.setGraphic(avatar);
            } else {
                // Текстовая иконка если аватарки нет
                String initials = group.getName().length() >= 2
                        ? group.getName().substring(0, 2).toUpperCase()
                        : group.getName().substring(0, 1).toUpperCase();
                Label lbl = new Label(initials);
                lbl.getStyleClass().add("group-text");
                btn.setGraphic(lbl);
            }

            // Tooltip с названием группы
            Tooltip tooltip = new Tooltip(group.getName());
            tooltip.setStyle("""
            -fx-background-color: #13112b;
            -fx-text-fill: white;
            -fx-font-size: 12px;
            -fx-background-radius: 6;
            -fx-padding: 4 8;
        """);
            tooltip.setShowDelay(javafx.util.Duration.millis(300));
            Tooltip.install(btn, tooltip);

            btn.setOnAction(e -> openGroup(group));
            groupsPane.getChildren().add(btn);
        }

        // Кнопка создать группу
        Button addBtn = new Button("+");
        addBtn.getStyleClass().add("group-add-btn");
        Tooltip addTooltip = new Tooltip("Создать сообщество");
        addTooltip.setStyle("""
        -fx-background-color: #13112b;
        -fx-text-fill: white;
        -fx-font-size: 12px;
        -fx-background-radius: 6;
        -fx-padding: 4 8;
    """);
        Tooltip.install(addBtn, addTooltip);
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
            ctrl.setGeneralController(generalController);

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

            List<String> textChs = java.util.Arrays.stream(
                            channelsField.getText().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(java.util.stream.Collectors.toList());

            List<String> voiceChs = java.util.Arrays.stream(
                            voiceField.getText().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(java.util.stream.Collectors.toList());

            new Thread(() -> {
                try {
                    Map<String, Object> body = new java.util.HashMap<>();
                    body.put("name", name);
                    body.put("textChannels", textChs);
                    body.put("voiceChannels", voiceChs);

                    String json = new ObjectMapper().writeValueAsString(body);
                    String response = ApiClient.postRaw("/api/groups", json);
                    Map<String, Object> result = new ObjectMapper().readValue(
                            response, new com.fasterxml.jackson.core.type.TypeReference<>() {});

                    Platform.runLater(() -> {
                        if (result.containsKey("error")) {
                            errorLabel.setText((String) result.get("error"));
                        } else {
                            popup.hide();
                            loadGroupsFromServer();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        });

        content.getChildren().addAll(title, nameField, channelsLabel, channelsField,
                voiceLabel, voiceField, errorLabel, createBtn);
        popup.getContent().add(content);

        var bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        popup.show(anchor.getScene().getWindow(), bounds.getMaxX() + 8, bounds.getMinY());
    }

    public static boolean isCurrentUserAdminInGroup(String channelId) {
        // Проверяем все группы текущего пользователя
        for (Group g : instance.groups) {
            boolean channelInGroup = g.getTextChannels().stream()
                    .anyMatch(ch -> ch.getId().equals(channelId));
            if (channelInGroup) {
                return g.getMembers().stream()
                        .anyMatch(m -> m.getUsername().equals(ApiClient.getCurrentUsername())
                                && m.isAdmin());
            }
        }
        return false;
    }

}