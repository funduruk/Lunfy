package ru.funduruk.model;

import lombok.Data;

@Data
public class UserProfile {
    private static UserProfile instance;

    private String username = "user-1";
    private String bio = "";
    private String status = "Онлайн";
    private String avatarPath = null;

    private UserProfile() {}

    public static UserProfile getInstance() {
        if (instance == null) instance = new UserProfile();
        return instance;
    }

}