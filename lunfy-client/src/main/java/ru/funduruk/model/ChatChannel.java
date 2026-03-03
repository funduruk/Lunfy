package ru.funduruk.model;

import lombok.Getter;
import lombok.Setter;

import java.nio.channels.Channel;

@Getter
@Setter
public class ChatChannel{
    private String id;
    private String name;
    private boolean voice;

    public ChatChannel(String id, String name, boolean voice) {
        this.id = id;
        this.name = name;
        this.voice = voice;
    }

}
