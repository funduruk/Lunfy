package ru.funduruk.model;

import lombok.Data;

@Data
public class Friend {
    private String username;
    private String tag;
    private String status;
    private boolean online;
    private long friendshipId;

    public Friend(String username, String tag, String status, boolean online, long friendshipId) {
        this.username = username;
        this.tag = tag;
        this.status = status;
        this.online = online;
        this.friendshipId = friendshipId;
    }


}