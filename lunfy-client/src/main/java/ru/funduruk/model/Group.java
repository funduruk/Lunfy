package ru.funduruk.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Group {
    private String id;
    private String name;
    private String avatarPath;
    private List<ChatChannel> textChannels = new ArrayList<>();
    private List<ChatChannel> voiceChannels = new ArrayList<>();
    private List<GroupMember> members = new ArrayList<>();

    public Group(String id, String name) {
        this.id = id;
        this.name = name;
    }
}