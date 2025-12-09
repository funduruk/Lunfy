package ru.funduruk.meetgridServer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "voice_participants")
public class VoiceParticipant {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private VoiceSession session;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private boolean isSharingScreen = false;
}

