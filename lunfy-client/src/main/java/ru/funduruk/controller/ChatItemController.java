package ru.funduruk.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class ChatItemController {

    @FXML private HBox root;
    @FXML private Label chatName;
    @FXML private Label lastMessage;

    private long chatId;

    public void setData(long chatId, String name, String lastMsg) {
        this.chatId = chatId;
        chatName.setText(name);
        lastMessage.setText(lastMsg != null ? lastMsg : "No messages yet");
    }

    public HBox getRoot() {
        return root;
    }

    public long getChatId() {
        return chatId;
    }
}