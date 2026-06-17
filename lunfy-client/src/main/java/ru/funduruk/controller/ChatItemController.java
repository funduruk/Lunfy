//package ru.funduruk.controller;
//
//import javafx.fxml.FXML;
//import javafx.scene.control.Label;
//import javafx.scene.layout.HBox;
//import lombok.Getter;
//
//public class ChatItemController {
//
//    @Getter
//    @FXML private HBox root;
//
//    @FXML private Label chatName;
//    @FXML private Label lastMessage;
//
//    @Getter
//    private long chatId;
//
//    public void setData(long chatId, String name, String lastMsg) {
//        this.chatId = chatId;
//        chatName.setText(name);
//        lastMessage.setText(lastMsg != null ? lastMsg : "No messages yet");
//    }
//
//}