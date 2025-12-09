package ru.funduruk.model;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class VoicePacket {
    private UUID sessionId;
    private UUID userId;
    private byte[] audioData;
    private byte[] screenData;
    private boolean isScreen;
}

