package ru.funduruk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import lombok.Setter;
import ru.funduruk.model.ChatChannel;
import ru.funduruk.model.Group;
import ru.funduruk.model.GroupMember;
import ru.funduruk.model.MessageStore;
import ru.funduruk.net.ApiClient;
import ru.funduruk.net.ChatEventBus;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Map;

public class GroupViewController {

    @FXML private Label groupNameLabel;

    private Group group;
    @Setter private GeneralController generalController;

    public void setGroup(Group group) {
        this.group = group;
        groupNameLabel.setText(group.getName());
        renderChannels();
        loadMembersFromServer(group.getId());

        if (!group.getTextChannels().isEmpty()) {
            openChannel(group.getTextChannels().getFirst());
        }

        ChatEventBus.setOnGroupMemberUpdate((type, data) -> {
            String groupId = String.valueOf(data.get("groupId"));
            if (!groupId.equals(group.getId())) return;

            Platform.runLater(() -> {
                if ("GROUP_MEMBER_KICKED".equals(type)) {
                    String kicked = (String) data.get("username");
                    // if kicked - leave
                    if (kicked.equals(ApiClient.getCurrentUsername())) {
                        if (generalController != null) {
                            generalController.setContent(new javafx.scene.layout.StackPane());
                        }
                    } else {
                        loadMembersFromServer(group.getId());
                    }
                } else if ("GROUP_ROLE_CHANGED".equals(type)) {
                    loadMembersFromServer(group.getId());
                }
            });
        });
    }

    @FXML private VBox textChannelList;
    @FXML private VBox voiceChannelList;

    private void renderChannels() {
        textChannelList.getChildren().clear();
        voiceChannelList.getChildren().clear();

        for (ChatChannel ch : group.getTextChannels()) {
            textChannelList.getChildren().add(buildChannelItem(ch));
        }
        for (ChatChannel ch : group.getVoiceChannels()) {
            voiceChannelList.getChildren().add(buildChannelItem(ch));
        }

        // Buttons add channels only for admin
        // Show always, check role after load members
        Button addText = new Button("+ Текстовый канал");
        addText.setStyle("-fx-background-color: transparent; -fx-text-fill: #aaa; -fx-font-size: 11px; -fx-cursor: hand;");
        addText.setOnAction(e -> showAddChannelPopup(false));
        addText.setVisible(false);
        addText.setId("addTextBtn");
        textChannelList.getChildren().add(addText);

        Button addVoice = new Button("+ Голосовой канал");
        addVoice.setStyle("-fx-background-color: transparent; -fx-text-fill: #aaa; -fx-font-size: 11px; -fx-cursor: hand;");
        addVoice.setOnAction(e -> showAddChannelPopup(true));
        addVoice.setVisible(false);
        addVoice.setId("addVoiceBtn");
        voiceChannelList.getChildren().add(addVoice);
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

    @FXML private StackPane chatPane;

    private void openChannel(ChatChannel channel) {
        if (!channel.isVoice()) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/ChatView.fxml")
                );
                Parent view = loader.load();
                ChatTabController ctrl = loader.getController();

                // load history
                new Thread(() -> {
                    try {
                        String response = ApiClient.getRaw("/api/chats/" + channel.getId() + "/messages");
                        ObjectMapper mapper = new ObjectMapper();
                        List<Map<String, Object>> messages = mapper.readValue(response,
                                new com.fasterxml.jackson.core.type.TypeReference<>() {});

                        Platform.runLater(() -> {
                            MessageStore.getInstance().clearChat(channel.getId());
                            MessageStore.getInstance().ensureChat(channel.getId());

                            for (Map<String, Object> m : messages) {
                                org.funduruk.dto.MessageDTO msg = new org.funduruk.dto.MessageDTO();
                                msg.setChatId(channel.getId());
                                msg.setSender((String) m.get("sender"));
                                msg.setText((String) m.get("text"));
                                msg.setMine(ApiClient.getCurrentUsername().equals(msg.getSender()));
                                msg.setId(((Number) m.getOrDefault("id", 0)).longValue());

                                String ts = (String) m.get("timestamp");
                                try {
                                    msg.setTimestamp(java.time.LocalDateTime.parse(ts)
                                            .atZone(java.time.ZoneId.systemDefault())
                                            .toInstant().toEpochMilli());
                                } catch (Exception ignored) {}

                                MessageStore.getInstance().addMessage(channel.getId(), msg);
                            }

                            ctrl.setChatInfo(channel.getName(), null);
                            ctrl.openChat(channel.getId());
                            chatPane.getChildren().setAll(view);
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            ctrl.setChatInfo(channel.getName(), null);
                            ctrl.openChat(channel.getId());
                            chatPane.getChildren().setAll(view);
                        });
                    }
                }).start();

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Label placeholder = new Label("🔊 Голосовой канал — скоро!");
            placeholder.setStyle("-fx-text-fill: #aaa; -fx-font-size: 16px; -fx-alignment: center;");
            chatPane.getChildren().setAll(placeholder);
        }
    }

    private void showAddChannelPopup(boolean isVoice) {
        javafx.stage.Popup popup = new javafx.stage.Popup();
        popup.setAutoHide(true);

        VBox content = new VBox(8);
        content.setStyle("-fx-background-color: #2d2b40; -fx-padding: 10; -fx-background-radius: 8;");
        content.setPrefWidth(200);

        Label title = new Label(isVoice ? "Новый голосовой канал" : "Новый текстовый канал");
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        TextField nameField = new TextField();
        nameField.setPromptText("Название канала");
        nameField.setStyle("-fx-background-color: #1e1b2e; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6;");

        Button createBtn = new Button("Создать");
        createBtn.setStyle("-fx-background-color: #5865f2; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
        createBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (name.isBlank()) return;
            addChannelToServer(name, isVoice);
            popup.hide();
        });

        content.getChildren().addAll(title, nameField, createBtn);
        popup.getContent().add(content);

        Node anchor = isVoice ? voiceChannelList : textChannelList;
        var bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        popup.show(anchor, bounds.getMinX(), bounds.getMaxY());
    }

    private void addChannelToServer(String name, boolean isVoice) {
        new Thread(() -> {
            try {
                String type = isVoice ? "VOICE" : "TEXT";
                ApiClient.postRaw("/api/groups/" + group.getId() + "/channels",
                        new ObjectMapper().writeValueAsString(Map.of("name", name, "type", type)));
                Platform.runLater(this::reloadChannels);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void reloadChannels() {
        new Thread(() -> {
            try {
                String response = ApiClient.getRaw("/api/groups/" + group.getId() + "/channels");
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, Object>> data = mapper.readValue(response,
                        new com.fasterxml.jackson.core.type.TypeReference<>() {});

                Platform.runLater(() -> {
                    group.getTextChannels().clear();
                    group.getVoiceChannels().clear();
                    for (Map<String, Object> ch : data) {
                        String chId = String.valueOf(ch.get("id"));
                        String chName = (String) ch.get("name");
                        boolean voice = "VOICE".equals(ch.get("type"));
                        ChatChannel channel = new ChatChannel(chId, chName, voice);
                        if (voice) group.getVoiceChannels().add(channel);
                        else group.getTextChannels().add(channel);
                    }
                    renderChannels();
                    updateAdminButtons();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadMembersFromServer(String groupId) {
        new Thread(() -> {
            try {
                String response = ApiClient.getRaw("/api/groups/" + groupId + "/members");
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, Object>> data = mapper.readValue(response,
                        new com.fasterxml.jackson.core.type.TypeReference<>() {});

                Platform.runLater(() -> {
                    group.getMembers().clear();
                    for (Map<String, Object> m : data) {
                        group.getMembers().add(new GroupMember(
                                (String) m.get("username"),
                                (String) m.get("role"),
                                true
                        ));
                    }
                    renderMembers();
                    updateAdminButtons();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML private VBox adminsList;
    @FXML private VBox membersList;
    @FXML private Label membersCountLabel;

    private void renderMembers() {
        adminsList.getChildren().clear();
        membersList.getChildren().clear();

        int total = group.getMembers().size();
        if (membersCountLabel != null) {
            membersCountLabel.setText(total + " " + (total == 1 ? "участник" : "участника"));
        }

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

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.getChildren().addAll(dot, name, spacer);

        boolean isMe = member.getUsername().equals(ApiClient.getCurrentUsername());

        if (isMe) {
            row.setOnMouseClicked(e -> {
                if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                    ContextMenu menu = new ContextMenu();
                    MenuItem leaveItem = new MenuItem("🚪 Покинуть группу");
                    leaveItem.setStyle("-fx-text-fill: #ed4245;");
                    leaveItem.setOnAction(ev -> leaveGroup());
                    menu.getItems().add(leaveItem);
                    menu.show(row, e.getScreenX(), e.getScreenY());
                }
            });
        } else if (isCurrentUserAdmin()) {
            row.setOnMouseClicked(e -> {
                if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                    ContextMenu menu = new ContextMenu();

                    MenuItem toggleRole = new MenuItem(
                            member.isAdmin() ? "Снять роль админа" : "Сделать админом");
                    toggleRole.setOnAction(ev -> toggleMemberRole(member));

                    MenuItem kick = new MenuItem("Исключить");
                    kick.setStyle("-fx-text-fill: #ed4245;");
                    kick.setOnAction(ev -> kickMember(member));

                    menu.getItems().addAll(toggleRole, new SeparatorMenuItem(), kick);
                    menu.show(row, e.getScreenX(), e.getScreenY());
                }
            });
        }

        return row;
    }

    private boolean isCurrentUserAdmin() {
        return group.getMembers().stream()
                .anyMatch(m -> m.getUsername().equals(ApiClient.getCurrentUsername()) && m.isAdmin());
    }

    private void updateAdminButtons() {
        textChannelList.getChildren().stream()
                .filter(n -> "addTextBtn".equals(n.getId()))
                .findFirst()
                .ifPresent(n -> n.setVisible(isCurrentUserAdmin()));

        voiceChannelList.getChildren().stream()
                .filter(n -> "addVoiceBtn".equals(n.getId()))
                .findFirst()
                .ifPresent(n -> n.setVisible(isCurrentUserAdmin()));
    }

    @FXML private VBox membersPanel;

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

                        boolean alreadyMember = group.getMembers().stream()
                                .anyMatch(m -> m.getUsername().equals(username));
                        if (alreadyMember) continue;

                        anyFriend = true;
                        Button btn = new Button(username + "#" + tag);
                        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 4 8;");
                        btn.setMaxWidth(Double.MAX_VALUE);
                        btn.setOnAction(e -> {
                            addMemberToServer(username, tag);
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

                    Node anchor = membersPanel != null ? membersPanel : groupNameLabel;
                    var bounds = anchor.localToScreen(anchor.getBoundsInLocal());
                    popup.show(anchor, bounds.getMinX(), bounds.getMinY() + 40);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void addMemberToServer(String username, String tag) {
        new Thread(() -> {
            try {
                ApiClient.postRaw("/api/groups/" + group.getId() + "/members",
                        new ObjectMapper().writeValueAsString(Map.of("username", username, "tag", tag)));
                Platform.runLater(() -> loadMembersFromServer(group.getId()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void toggleMemberRole(GroupMember member) {
        String newRole = member.isAdmin() ? "MEMBER" : "ADMIN";
        new Thread(() -> {
            try {
                ApiClient.postRaw("/api/groups/" + group.getId() + "/members/" + member.getUsername() + "/role",
                        new ObjectMapper().writeValueAsString(Map.of("role", newRole)));
                Platform.runLater(() -> loadMembersFromServer(group.getId()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void kickMember(GroupMember member) {
        new Thread(() -> {
            try {
                ApiClient.postRaw("/api/groups/" + group.getId() + "/members/" + member.getUsername() + "/kick", "{}");
                Platform.runLater(() -> loadMembersFromServer(group.getId()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML private Button hideChannelsBtn;
    @FXML private Button hideMembersBtn;
    @FXML private VBox channelPanel;

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
            channelPanel.setPrefWidth(32);
            channelPanel.setMinWidth(32);
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

    @FXML
    private void openGroupSettings() {
        javafx.stage.Popup popup = new javafx.stage.Popup();
        popup.setAutoHide(true);

        VBox content = new VBox(10);
        content.setStyle("""
        -fx-background-color: #1e1b35;
        -fx-padding: 14;
        -fx-background-radius: 10;
        -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 4);
    """);
        content.setPrefWidth(220);

        Label title = new Label("Настройки группы");
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label nameLabel = new Label("Название");
        nameLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11px;");

        TextField nameField = new TextField(group.getName());
        nameField.setStyle("""
        -fx-background-color: rgba(255,255,255,0.07);
        -fx-text-fill: white;
        -fx-background-radius: 8;
        -fx-border-radius: 8;
        -fx-border-color: rgba(255,255,255,0.1);
        -fx-padding: 6 10;
    """);

        Button saveBtn = new Button("Сохранить");
        saveBtn.setStyle("""
        -fx-background-color: #5865f2;
        -fx-text-fill: white;
        -fx-background-radius: 8;
        -fx-padding: 6 16;
        -fx-cursor: hand;
    """);
        saveBtn.setOnAction(e -> {
            String newName = nameField.getText().trim();
            if (!newName.isBlank()) {
                updateGroupName(newName);
                popup.hide();
            }
        });

        if (isCurrentUserAdmin()) {
            Button deleteBtn = new Button("🗑 Удалить группу");
            deleteBtn.setStyle("""
            -fx-background-color: rgba(237,66,69,0.15);
            -fx-text-fill: #ed4245;
            -fx-background-radius: 8;
            -fx-padding: 6 16;
            -fx-cursor: hand;
            -fx-max-width: Infinity;
        """);
            deleteBtn.setOnAction(e -> {
                deleteGroup();
                popup.hide();
            });

            Button avatarBtn = new Button("🖼 Загрузить аватарку");
            avatarBtn.setStyle("""
    -fx-background-color: rgba(255,255,255,0.07);
    -fx-text-fill: white;
    -fx-background-radius: 8;
    -fx-padding: 6 16;
    -fx-cursor: hand;
""");
            avatarBtn.setOnAction(e -> {
                javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
                chooser.setTitle("Выбери аватарку группы");
                chooser.getExtensionFilters().add(
                        new javafx.stage.FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg")
                );
                java.io.File file = chooser.showOpenDialog(groupNameLabel.getScene().getWindow());
                if (file != null) {
                    uploadGroupAvatar(file);
                }
            });

            content.getChildren().addAll(title, nameLabel, nameField, avatarBtn, saveBtn, deleteBtn);

        } else {
            content.getChildren().addAll(title, nameLabel, nameField, saveBtn);
        }

        popup.getContent().add(content);
        var bounds = groupNameLabel.localToScreen(groupNameLabel.getBoundsInLocal());
        popup.show(groupNameLabel, bounds.getMinX(), bounds.getMaxY() + 4);
    }

    private void updateGroupName(String newName) {
        new Thread(() -> {
            try {
                ApiClient.postRaw("/api/groups/" + group.getId() + "/settings",
                        new ObjectMapper().writeValueAsString(Map.of("name", newName)));
                Platform.runLater(() -> {
                    group.setName(newName);
                    groupNameLabel.setText(newName);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void deleteGroup() {
        new Thread(() -> {
            try {
                ApiClient.delete("/api/groups/" + group.getId());
                Platform.runLater(() -> {
                    if (generalController != null) {
                        generalController.setContent(new javafx.scene.layout.StackPane());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void leaveGroup() {
        new Thread(() -> {
            try {
                String response = ApiClient.postRaw(
                        "/api/groups/" + group.getId() + "/members/" +
                                ApiClient.getCurrentUsername() + "/kick", "{}");
                System.out.println("leaveGroup response: " + response);
                Platform.runLater(() -> {
                    if (generalController != null) {
                        generalController.setContent(new javafx.scene.layout.StackPane());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void uploadGroupAvatar(java.io.File file) {
        new Thread(() -> {
            try {
                // Multipart upload
                String boundary = "----FormBoundary" + System.currentTimeMillis();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("http://localhost:8080/api/groups/" + group.getId() + "/avatar"))
                        .header("Authorization", "Bearer " + ApiClient.getToken())
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(buildMultipartBody(file, boundary))
                        .build();

                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpResponse<String> response = client.send(request,
                        java.net.http.HttpResponse.BodyHandlers.ofString());

                System.out.println("Avatar upload: " + response.body());

                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> result = mapper.readValue(response.body(),
                        new com.fasterxml.jackson.core.type.TypeReference<>() {
                        });

                if (result.containsKey("avatarPath")) {
                    String avatarPath = (String) result.get("avatarPath");
                    Platform.runLater(() -> {
                        group.setAvatarPath(avatarPath);

                        if (GroupsTabController.getInstance() != null) {
                            GroupsTabController.getInstance().loadGroupsFromServer();
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private HttpRequest.BodyPublisher buildMultipartBody(File file, String boundary) throws IOException {
        byte[] fileBytes = java.nio.file.Files.readAllBytes(file.toPath());
        String header = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n" +
                "Content-Type: image/png\r\n\r\n";
        String footer = "\r\n--" + boundary + "--\r\n";

        byte[] headerBytes = header.getBytes();
        byte[] footerBytes = footer.getBytes();
        byte[] body = new byte[headerBytes.length + fileBytes.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, body, 0, headerBytes.length);
        System.arraycopy(fileBytes, 0, body, headerBytes.length, fileBytes.length);
        System.arraycopy(footerBytes, 0, body, headerBytes.length + fileBytes.length, footerBytes.length);

        return java.net.http.HttpRequest.BodyPublishers.ofByteArray(body);
    }
}