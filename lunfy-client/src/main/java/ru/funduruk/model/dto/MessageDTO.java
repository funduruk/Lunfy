package ru.funduruk.model.dto;

public class MessageDTO {
    private String chatId;
    private String sender;
    private String text;
    private boolean isMine;
    private long timeStamp;

    public MessageDTO(String chatId, String sender, String text, boolean isMine) {
        this.chatId = chatId;
        this.sender = sender;
        this.text = text;
        this.isMine = isMine;
    }

    public String getChatId() { return chatId; }
    public String getSender() { return sender; }
    public String getText() { return text; }
    public boolean isMine() { return isMine; }
}