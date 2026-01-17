package ru.funduruk.Entity;

import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class VoiceParticipant {
    private UUID userId;
    private Channel channel;
    private boolean sharingScreen = false;
}
