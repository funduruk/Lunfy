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
        addBtn.setOnAction(e -> System.out.println("TODO: создать группу"));
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
}