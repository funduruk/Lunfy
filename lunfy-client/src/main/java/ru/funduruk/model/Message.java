package ru.funduruk.model;


import java.time.LocalDateTime;

public class Message {

    private long id;
    private User sender;
    private String text;
    private LocalDateTime time;

    public Message(long id, User sender, String text, LocalDateTime time) {
        this.id = id;
        this.sender = sender;
        this.text = text;
        this.time = time;
    }

    public long getId() {
        return id;
    }

    public User getSender() {
        return sender;
    }

    public String getText() {
        return text;
    }

    public LocalDateTime getTime() {
        return time;
    }
}

