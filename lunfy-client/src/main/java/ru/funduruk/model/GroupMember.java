package ru.funduruk.model;

import lombok.Data;

@Data
public class GroupMember {
    private String username;
    private String role;
    private boolean online;

    public GroupMember(String username, String role, boolean online) {
        this.username = username;
        this.role = role;
        this.online = online;
    }

    public boolean isAdmin(){
        return role.equals("ADMIN");
    }

}
