package ru.funduruk.model;

public class User {

    private long id;
    private String username;
    private String avatarPath;

    public User(long id, String username, String avatarPath) {
        this.id = id;
        this.username = username;
        this.avatarPath = avatarPath;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getAvatarPath() {
        return avatarPath;
    }
}
