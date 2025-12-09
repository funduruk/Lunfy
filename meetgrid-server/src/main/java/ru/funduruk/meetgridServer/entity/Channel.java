package ru.funduruk.meetgridServer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ru.funduruk.meetgridServer.entity.enums.ChannelType;

import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "channels")
public class Channel {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    private String name;

    @Enumerated(EnumType.STRING)
    private ChannelType type;

    @ManyToOne
    @JoinColumn(name = "server_id")
    private Server server;

    @OneToMany(mappedBy = "channel")
    private Set<Message> messages;

    @OneToMany(mappedBy = "channel")
    private Set<VoiceSession> voiceSessions;
}

