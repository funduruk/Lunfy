    package ru.funduruk.controller;

    import com.fasterxml.jackson.databind.ObjectMapper;
    import javafx.application.Platform;
    import javafx.fxml.FXML;
    import javafx.fxml.FXMLLoader;
    import javafx.scene.Parent;
    import javafx.scene.control.Button;
    import javafx.scene.control.Label;
    import javafx.scene.control.ScrollPane;
    import javafx.scene.control.TextField;
    import javafx.scene.layout.*;
    import javafx.stage.Stage;
    import lombok.Getter;
    import ru.funduruk.manager.NotificationManager;
    import ru.funduruk.manager.SceneManager;
    import ru.funduruk.model.GroupDM;
    import ru.funduruk.model.MessageStore;
    import ru.funduruk.model.UserProfile;
    import ru.funduruk.net.ApiClient;
    import ru.funduruk.net.ChatEventBus;
    import ru.funduruk.net.WSClient;

    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;


    public class GeneralController {

        @FXML private Label profileUsername;

        @Getter private static GeneralController instance;


        @FXML
        public void initialize() throws Exception {
            instance = this;
            enableWindowDragging();
            enableWindowResize();

            UserProfile profile = UserProfile.getInstance();
            profileUsername.setText(profile.getUsername() + "#" + profile.getTag());
            updateStatusDisplay(profile.getStatus());

            loadChatView();
            loadGroupsView();

            WSClient.connect(ApiClient.WS_BASE + "/ws?username=" + ApiClient.getCurrentUsername()
                    + "&token=" + ApiClient.getToken());

            ChatEventBus.setOnGroupJoined(data -> Platform.runLater(() -> {
                GroupsTabController gtc = GroupsTabController.getInstance();
                if (gtc != null) gtc.loadGroupsFromServer();
            }));

            ChatEventBus.setOnChannelCreated(data -> Platform.runLater(() -> {
                GroupsTabController gtc = GroupsTabController.getInstance();
                if (gtc != null) gtc.loadGroupsFromServer();
            }));

            ChatEventBus.setOnGroupInvite(data -> Platform.runLater(() -> {
                String groupName = (String) data.get("groupName");
                String invitedBy = (String) data.get("invitedBy");

                if (NotificationManager.isWindowHidden()) {
                    NotificationManager.showSystem("Приглашение в сообщество",
                            invitedBy + " приглашает вас в «" + groupName + "»");
                }
            }));

            ChatEventBus.setOnReconnect(() -> Platform.runLater(() -> {
                System.out.println(">>> WS восстановлен, перезагружаем открытый чат");

                if (chatController == null) return;
                String currentChatId = chatController.getCurrentChatId();
                if (currentChatId == null) return;

                openChat(currentChatId);
            }));

            ChatEventBus.setOnCallOffer(signal -> {
                Platform.runLater(() -> {
                    if (NotificationManager.isWindowHidden()) {
                        NotificationManager.showSystem(
                                "Входящий вызов", "Звонок от " + signal.getFromUser());
                        Stage st = (Stage) generalRoot.getScene().getWindow();
                        st.setIconified(false);
                        st.toFront();
                    }
                    openCall(ctrl -> ctrl.initIncoming(signal));
                });
            });


            loadChatsFromFriends();
            setupCallResize();
        }

        @FXML private javafx.scene.shape.Circle statusDot;
        @FXML private Label profileStatus;

        public void updateStatusDisplay(String status) {
            if (profileStatus != null) {
                profileStatus.setText(status);
            }
            if (statusDot != null) {
                String color = switch (status == null ? "Онлайн" : status) {
                    case "Не беспокоить" -> "#ed4245";
                    case "Офлайн" -> "#747f8d";
                    default -> "#3ba55d";
                };
                statusDot.setFill(javafx.scene.paint.Color.web(color));
            }
        }

        private Parent chatView;

        private void loadChatsFromFriends() {
            new Thread(() -> {
                try {
                    String response = ApiClient.getRaw("/api/friends");
                    ObjectMapper mapper = new ObjectMapper();
                    List<Map<String, Object>> friends = mapper.readValue(response,
                            new com.fasterxml.jackson.core.type.TypeReference<>() {});

                    Platform.runLater(() -> {
                        for (Map<String, Object> f : friends) {
                            String friendUsername = (String) f.get("username");
                            String chatId = buildDmChatId(
                                    ApiClient.getCurrentUsername(), friendUsername
                            );
                            addChatIfAbsent(chatId, friendUsername);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }

        private String buildDmChatId(String user1, String user2) {
            String[] users = {user1.toLowerCase(), user2.toLowerCase()};
            java.util.Arrays.sort(users);
            return "dm-" + users[0] + "-" + users[1];
        }

        @FXML private StackPane contentPane;

        private ChatTabController chatController;

        private void loadChatView() {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/ChatView.fxml")
                );
                chatView = loader.load();
                chatController = loader.getController();
                contentPane.getChildren().setAll(chatView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @FXML private void openFriends() {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/FriendsView.fxml")
                );
                Parent view = loader.load();
                contentPane.getChildren().setAll(view);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @FXML private BorderPane generalRoot;

        @FXML private void close() {
            Stage stage = (Stage) generalRoot.getScene().getWindow();
            stage.hide();
        }

        @FXML private void minimize() {
            ((Stage) generalRoot.getScene().getWindow()).setIconified(true);
        }

        private double prevX, prevY, prevWidth, prevHeight;
        private boolean maximized = false;

        @FXML private void maximize() {
            Stage stage = (Stage) generalRoot.getScene().getWindow();

            if (!maximized) {
                prevX = stage.getX();
                prevY = stage.getY();
                prevWidth = stage.getWidth();
                prevHeight = stage.getHeight();

                javafx.geometry.Rectangle2D bounds =
                        javafx.stage.Screen.getPrimary().getVisualBounds();
                stage.setX(bounds.getMinX());
                stage.setY(bounds.getMinY());
                stage.setWidth(bounds.getWidth());
                stage.setHeight(bounds.getHeight());

                maximized = true;
            } else {
                stage.setX(prevX);
                stage.setY(prevY);
                stage.setWidth(prevWidth);
                stage.setHeight(prevHeight);

                maximized = false;
            }
        }

        @FXML private HBox titleBar;

        private double xOffset, yOffset;

        private void enableWindowDragging() {
            titleBar.setOnMousePressed(e -> {
                xOffset = e.getSceneX();
                yOffset = e.getSceneY();
            });
            titleBar.setOnMouseDragged(e -> {
                Stage stage = (Stage) titleBar.getScene().getWindow();
                stage.setX(e.getScreenX() - xOffset);
                stage.setY(e.getScreenY() - yOffset);
            });
        }

        private boolean isResizing = false;

        private void enableWindowResize() {
            final int RESIZE_MARGIN = 8;

            generalRoot.setOnMouseMoved(e -> {
                double w = generalRoot.getWidth();
                double h = generalRoot.getHeight();
                double x = e.getX();
                double y = e.getY();

                boolean right = x > w - RESIZE_MARGIN;
                boolean bottom = y > h - RESIZE_MARGIN;

                if (right && bottom) {
                    generalRoot.setCursor(javafx.scene.Cursor.SE_RESIZE);
                } else if (right) {
                    generalRoot.setCursor(javafx.scene.Cursor.E_RESIZE);
                } else if (bottom) {
                    generalRoot.setCursor(javafx.scene.Cursor.S_RESIZE);
                } else {
                    generalRoot.setCursor(javafx.scene.Cursor.DEFAULT);
                }
            });

            generalRoot.setOnMousePressed(e -> {
                javafx.scene.Cursor cursor = generalRoot.getCursor();
                isResizing = cursor == javafx.scene.Cursor.SE_RESIZE
                        || cursor == javafx.scene.Cursor.E_RESIZE
                        || cursor == javafx.scene.Cursor.S_RESIZE;
            });

            generalRoot.setOnMouseDragged(e -> {
                if (!isResizing) return;

                Stage stage = (Stage) generalRoot.getScene().getWindow();
                javafx.scene.Cursor cursor = generalRoot.getCursor();

                double newWidth = e.getScreenX() - stage.getX();
                double newHeight = e.getScreenY() - stage.getY();

                if (cursor == javafx.scene.Cursor.SE_RESIZE || cursor == javafx.scene.Cursor.E_RESIZE) {
                    if (newWidth >= stage.getMinWidth()) {
                        stage.setWidth(newWidth);
                    }
                }
                if (cursor == javafx.scene.Cursor.SE_RESIZE || cursor == javafx.scene.Cursor.S_RESIZE) {
                    if (newHeight >= stage.getMinHeight()) {
                        stage.setHeight(newHeight);
                    }
                }
            });

            generalRoot.setOnMouseReleased(e -> isResizing = false);
        }

        public void openProfile() {
            SceneManager.setScene("/fxml/ProfileView.fxml", "/css/style.css");
        }

        public void setContent(Parent view) {
            contentPane.getChildren().setAll(view);
        }

        @FXML private VBox groupsPane;

        private void loadGroupsView() {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/GroupsTab.fxml")
                );
                Parent view = loader.load();
                GroupsTabController ctrl = loader.getController();
                ctrl.setGeneralController(this);
                groupsPane.getChildren().setAll(view);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @FXML private VBox chatListPane;

        public void addChatIfAbsent(String chatId, String title) {
            boolean exists = chatListPane.getChildren().stream()
                    .anyMatch(n -> chatId.equals(n.getUserData()));

            if (!exists) {
                Label chat = new Label(title);
                chat.setUserData(chatId);
                chat.getStyleClass().add("chat-item");
                chat.setOnMouseClicked(e -> openChat(chatId));
                chatListPane.getChildren().add(chat);
                MessageStore.getInstance().ensureChat(chatId);
            }
        }

        public void openChat(String chatId) {
            contentPane.getChildren().setAll(chatView);
            contentPane.getChildren().setAll(chatView);

            clearUnread(chatId);

            if (chatId.startsWith("gdm-")) {
                if (groupDMViews.containsKey(chatId)) {
                    contentPane.getChildren().setAll(groupDMViews.get(chatId));
                } else {

                    loadGroupDM(chatId);
                }
                return;
            }

            String chatName = chatId;
            if (chatId.startsWith("dm-")) {
                String[] parts = chatId.replace("dm-", "").split("-");
                // uncorrected part user
                String currentUser = ApiClient.getCurrentUsername().toLowerCase();
                for (String part : parts) {
                    if (!part.equals(currentUser)) {
                        chatName = part;
                        break;
                    }
                }
            }

            final String finalChatName = chatName;
            chatController.setChatInfo(finalChatName, null);

            new Thread(() -> {
                try {
                    System.out.println("Loading history for: " + chatId);
                    String response = ApiClient.getRaw("/api/chats/" + chatId + "/messages");
                    System.out.println("History response: " + response);

                    ObjectMapper mapper = new ObjectMapper();
                    List<Map<String, Object>> messages = mapper.readValue(response,
                            new com.fasterxml.jackson.core.type.TypeReference<>() {});

                    Platform.runLater(() -> {
                        // clear hash before load
                        MessageStore.getInstance().clearChat(chatId);
                        MessageStore.getInstance().ensureChat(chatId);

                        for (Map<String, Object> m : messages) {
                            org.funduruk.dto.MessageDTO msg = new org.funduruk.dto.MessageDTO();
                            msg.setId(((Number) m.getOrDefault("id", 0)).longValue());
                            msg.setChatId(chatId);
                            msg.setSender((String) m.get("sender"));
                            msg.setText((String) m.get("text"));
                            msg.setMine(ApiClient.getCurrentUsername().equals(msg.getSender()));

                            String ts = (String) m.get("timestamp");
                            try {
                                msg.setTimestamp(java.time.LocalDateTime.parse(ts)
                                        .atZone(java.time.ZoneId.systemDefault())
                                        .toInstant().toEpochMilli());
                            } catch (Exception ignored) {}

                            MessageStore.getInstance().addMessage(chatId, msg);
                        }
                        chatController.openChat(chatId);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    // chat load in hash
                    Platform.runLater(() -> chatController.openChat(chatId));
                }
            }).start();
        }

        private void loadGroupDM(String chatId) {
            String groupId = chatId.replace("gdm-", "");

            new Thread(() -> {
                try {
                    String membersResp = ApiClient.getRaw("/api/groups/" + groupId + "/members");
                    String infoResp = ApiClient.getRaw("/api/groups/" + groupId);

                    ObjectMapper mapper = new ObjectMapper();
                    List<Map<String, Object>> members = mapper.readValue(membersResp,
                            new com.fasterxml.jackson.core.type.TypeReference<>() {});
                    Map<String, Object> info = mapper.readValue(infoResp,
                            new com.fasterxml.jackson.core.type.TypeReference<>() {});

                    String ownerUsername = (String) info.get("ownerUsername");

                    List<String> memberNames = members.stream()
                            .map(m -> (String) m.get("username"))
                            .collect(java.util.stream.Collectors.toList());

                    String chatName = chatListPane.getChildren().stream()
                            .filter(n -> chatId.equals(n.getUserData()))
                            .findFirst()
                            .map(n -> ((Label) n).getText())
                            .orElse(chatId);

                    Platform.runLater(() -> {
                        try {
                            GroupDM gdm = new GroupDM(chatId, chatName, new ArrayList<>(memberNames));
                            gdm.setOwnerUsername(ownerUsername);

                            FXMLLoader loader = new FXMLLoader(
                                    getClass().getResource("/fxml/GroupDMView.fxml"));
                            Parent view = loader.load();
                            GroupDMController ctrl = loader.getController();
                            ctrl.setGroupDM(gdm);
                            ctrl.setGeneralController(this);

                            groupDMViews.put(chatId, view);
                            groupDMControllers.put(chatId, ctrl);

                            contentPane.getChildren().setAll(view);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }

        public void removeChat(String chatId) {
            chatListPane.getChildren().removeIf(n -> chatId.equals(n.getUserData()));
        }

        private final Map<String, Parent> groupDMViews = new HashMap<>();
        private final Map<String, GroupDMController> groupDMControllers = new HashMap<>();
        @FXML private Button newGroupDMBtn;

        @FXML
        private void openNewGroupDM() {
            new Thread(() -> {
                try {
                    String response = ApiClient.getRaw("/api/friends");
                    ObjectMapper mapper = new ObjectMapper();
                    List<Map<String, Object>> friendsData = mapper.readValue(response,
                            new com.fasterxml.jackson.core.type.TypeReference<>() {});

                    Platform.runLater(() -> showGroupDMPopup(friendsData));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }

        private void showGroupDMPopup(List<Map<String, Object>> friendsData) {
            javafx.stage.Popup popup = new javafx.stage.Popup();
            popup.setAutoHide(true);

            VBox content = new VBox(8);
            content.setStyle("""
            -fx-background-color: #2d2b40;
            -fx-padding: 12;
            -fx-background-radius: 10;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 4);
        """);
            content.setPrefWidth(240);
            content.setMaxWidth(240);

            Label title = new Label("Новый групповой чат");
            title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");

            TextField nameField = new TextField();
            nameField.setPromptText("Название группы");
            nameField.setStyle("-fx-background-color: #1e1b2e; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6;");

            Label membersLabel = new Label("Выбери друзей:");
            membersLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11px;");

            List<String> selectedFriends = new ArrayList<>();
            VBox friendCheckboxes = new VBox(4);

            if (friendsData.isEmpty()) {
                Label empty = new Label("Нет друзей для добавления");
                empty.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");
                friendCheckboxes.getChildren().add(empty);
            } else {
                for (Map<String, Object> f : friendsData) {
                    String username = (String) f.get("username");
                    String tag = (String) f.get("tag");
                    String display = username + "#" + tag;

                    javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(display);
                    cb.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
                    cb.setPrefWidth(216);
                    cb.setMaxWidth(216);

                    cb.setOnAction(e -> {
                        if (cb.isSelected()) selectedFriends.add(username);
                        else selectedFriends.remove(username);
                    });
                    friendCheckboxes.getChildren().add(cb);
                }
            }

            Button createBtn = new Button("Создать");
            createBtn.setStyle("""
            -fx-background-color: #5865f2;
            -fx-text-fill: white;
            -fx-background-radius: 6;
            -fx-padding: 6 16;
            -fx-cursor: hand;
        """);
            createBtn.setOnAction(e -> {
                if (nameField.getText().isBlank() || selectedFriends.isEmpty()) return;
                createGroupDM(nameField.getText(), selectedFriends);
                popup.hide();
            });

            ScrollPane scrollPane = new ScrollPane(friendCheckboxes);
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefHeight(120);
            scrollPane.setMaxHeight(120);
            scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

            content.getChildren().addAll(title, nameField, membersLabel, scrollPane, createBtn);
            popup.getContent().add(content);

            var bounds = newGroupDMBtn.localToScreen(newGroupDMBtn.getBoundsInLocal());
            popup.show(newGroupDMBtn.getScene().getWindow(), bounds.getMinX() + 30, bounds.getMaxY() + 4);
        }

        private void createGroupDM(String name, List<String> members) {
            new Thread(() -> {
                try {
                    Map<String, Object> body = new java.util.HashMap<>();
                    body.put("name", name);
                    body.put("members", members);

                    String json = new ObjectMapper().writeValueAsString(body);
                    String response = ApiClient.postRaw("/api/groups/dm", json);
                    Map<String, Object> result = new ObjectMapper().readValue(
                            response, new com.fasterxml.jackson.core.type.TypeReference<>() {});

                    if (result.containsKey("error")) {
                        System.out.println("Ошибка: " + result.get("error"));
                        return;
                    }

                    String chatId = "gdm-" + result.get("id");
                    GroupDM gdm = new GroupDM(chatId, name, new ArrayList<>(members));
                    gdm.getMembers().add(ApiClient.getCurrentUsername());
                    gdm.setOwnerUsername(ApiClient.getCurrentUsername());

                    Platform.runLater(() -> {
                        try {
                            FXMLLoader loader = new FXMLLoader(
                                    getClass().getResource("/fxml/GroupDMView.fxml")
                            );
                            Parent view = loader.load();
                            GroupDMController ctrl = loader.getController();
                            ctrl.setGroupDM(gdm);
                            ctrl.setGeneralController(this);

                            addChatIfAbsent(chatId, name);
                            groupDMViews.put(chatId, view);
                            groupDMControllers.put(chatId, ctrl);

                            contentPane.getChildren().setAll(view);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }

        @FXML private StackPane callContainer;

        private Parent callView;
        private CallController callController;

        public void openCall(java.util.function.Consumer<CallController> init) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/CallView.fxml"));
                callView = loader.load();
                callController = loader.getController();

                var scene = callContainer.getScene();
                if (scene != null) {
                    String callCss = getClass().getResource("/css/call.css").toExternalForm();
                    if (!scene.getStylesheets().contains(callCss)) {
                        scene.getStylesheets().add(callCss);
                    }
                }

                init.accept(callController);

                callContainer.getChildren().setAll(callView);
                callContainer.setVisible(true);
                callContainer.setManaged(true);

                // show handle
                callResizeHandle.setVisible(true);
                callResizeHandle.setManaged(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        public void returnToChat() {
            callContainer.getChildren().clear();
            callContainer.setVisible(false);
            callContainer.setManaged(false);

            callResizeHandle.setVisible(false);
            callResizeHandle.setManaged(false);

            callView = null;
            callController = null;
        }

        public void setCallContainerHeight(double height) {
            if (height < 0) {
                callContainer.setPrefHeight(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
                callContainer.setMinHeight(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
                callContainer.setMaxHeight(Double.MAX_VALUE);
            } else {
                callContainer.setPrefHeight(height);
                callContainer.setMinHeight(height);
                callContainer.setMaxHeight(height);
            }
        }

        private final Map<String, Integer> unreadCounts = new HashMap<>();

        public void incrementUnread(String chatId) {
            unreadCounts.merge(chatId, 1, Integer::sum);
            updateChatBadge(chatId);
        }

        public void clearUnread(String chatId) {
            unreadCounts.remove(chatId);
            updateChatBadge(chatId);
        }

        private void updateChatBadge(String chatId) {
            for (javafx.scene.Node node : chatListPane.getChildren()) {
                if (chatId.equals(node.getUserData()) && node instanceof Label label) {
                    int count = unreadCounts.getOrDefault(chatId, 0);
                    String baseName = label.getText().replaceAll(" ●.*$", "");
                    if (count > 0) {
                        label.setText(baseName + "  ● " + count);
                        label.setStyle("-fx-font-weight: bold;");
                    } else {
                        label.setText(baseName);
                        label.setStyle("");
                    }
                }
            }
        }

        @FXML
        private void openInvites() {
            VBox root = new VBox(20);
            root.getStyleClass().add("invites-root");

            VBox header = new VBox(6);
            Label eyebrow = new Label("ВХОДЯЩИЕ");
            eyebrow.getStyleClass().add("settings-eyebrow");

            Label title = new Label("Приглашения в сообщества");
            title.getStyleClass().add("settings-title");

            Label subtitle = new Label("Принимайте приглашения от друзей или отклоняйте их");
            subtitle.getStyleClass().add("settings-subtitle");

            header.getChildren().addAll(eyebrow, title, subtitle);

            VBox invitesList = new VBox(12);
            invitesList.getStyleClass().add("invites-list");

            root.getChildren().addAll(header, invitesList);
            contentPane.getChildren().setAll(root);

            new Thread(() -> {
                try {
                    String response = ApiClient.getRaw("/api/groups/invites");
                    ObjectMapper mapper = new ObjectMapper();
                    List<Map<String, Object>> invites = mapper.readValue(response,
                            new com.fasterxml.jackson.core.type.TypeReference<>() {});

                    Platform.runLater(() -> {
                        invitesList.getChildren().clear();
                        if (invites.isEmpty()) {
                            Label empty = new Label("Нет новых приглашений");
                            empty.getStyleClass().add("invites-empty");
                            invitesList.getChildren().add(empty);
                        } else {
                            for (Map<String, Object> inv : invites) {
                                invitesList.getChildren().add(buildInviteCard(inv, invitesList));
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }

        private javafx.scene.Node buildInviteCard(Map<String, Object> inv, VBox parentList) {
            Long inviteId = ((Number) inv.get("inviteId")).longValue();
            String groupName = (String) inv.get("groupName");
            String invitedBy = (String) inv.get("invitedBy");

            HBox card = new HBox(16);
            card.getStyleClass().add("invite-card");
            card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            StackPane avatar = new StackPane();
            avatar.getStyleClass().add("invite-avatar");
            javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(24);
            circle.getStyleClass().add("invite-avatar-circle");
            Label initial = new Label(groupName != null && !groupName.isEmpty()
                    ? groupName.substring(0, 1).toUpperCase() : "?");
            initial.getStyleClass().add("invite-avatar-initial");
            avatar.getChildren().addAll(circle, initial);

            VBox info = new VBox(4);
            Label name = new Label(groupName);
            name.getStyleClass().add("invite-group-name");
            Label by = new Label("Пригласил: " + invitedBy);
            by.getStyleClass().add("invite-by");
            info.getChildren().addAll(name, by);

            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

            Button accept = new Button("Принять");
            accept.getStyleClass().add("primary-btn");
            accept.setOnAction(e -> respondInvite(inviteId, true, card, parentList));

            Button decline = new Button("Отклонить");
            decline.getStyleClass().add("secondary-btn");
            decline.setOnAction(e -> respondInvite(inviteId, false, card, parentList));

            card.getChildren().addAll(avatar, info, spacer, accept, decline);
            return card;
        }

        private void respondInvite(Long inviteId, boolean accept, javafx.scene.Node card, VBox parentList) {
            new Thread(() -> {
                try {
                    String path = "/api/groups/invites/" + inviteId + (accept ? "/accept" : "/decline");
                    ApiClient.postRaw(path, "{}");
                    Platform.runLater(() -> {
                        parentList.getChildren().remove(card);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }

        public boolean isInCall() {
            return callController != null;
        }

        public void endCurrentCall() {
            if (callController != null) {
                callController.forceEnd();
            }
        }

        @FXML private javafx.scene.layout.Region callResizeHandle;

        private double resizeStartY;
        private double resizeStartHeight;

        private void setupCallResize() {
            callResizeHandle.setOnMousePressed(e -> {
                resizeStartY = e.getScreenY();
                resizeStartHeight = callContainer.getHeight();
            });

            callResizeHandle.setOnMouseDragged(e -> {
                double delta = e.getScreenY() - resizeStartY;
                double newHeight = resizeStartHeight + delta;

                double sceneHeight = callContainer.getScene() != null
                        ? callContainer.getScene().getHeight()
                        : 800;
                double maxHeight = sceneHeight * 0.8;

                if (newHeight < 80) newHeight = 80;
                if (newHeight > maxHeight) newHeight = maxHeight;

                callContainer.setPrefHeight(newHeight);
                callContainer.setMinHeight(newHeight);
                callContainer.setMaxHeight(newHeight);
            });
        }

        @FXML
        private void openSettings() {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/SettingsView.fxml"));
                Parent view = loader.load();
                contentPane.getChildren().setAll(view);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }