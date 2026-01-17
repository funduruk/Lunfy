package ru.funduruk.Entity;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class VoiceSession {
    private UUID id = UUID.randomUUID();
    private Set<VoiceParticipant> participants = new HashSet<>();
}
