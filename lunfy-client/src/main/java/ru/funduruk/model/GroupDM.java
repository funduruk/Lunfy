package ru.funduruk.model;

import lombok.Data;
import java.util.List;

@Data
public class GroupDM {
    private String id;
    private String name;
    private String avatarPath;
    private List<String> members;

    private String ownerUsername;
    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String s) { this.ownerUsername = s; }

    public GroupDM(String id, String name, List<String> members) {
        this.id = id;
        this.name = name;
        this.members = members;
    }


    public void addMember(String username) { members.add(username); }
    public void removeMember(String username) { members.remove(username); }
}