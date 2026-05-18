package ru.funduruk.lunfyServer.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "channels")
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private ChannelType type = ChannelType.TEXT;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    public enum ChannelType {
        TEXT, VOICE
    }
}