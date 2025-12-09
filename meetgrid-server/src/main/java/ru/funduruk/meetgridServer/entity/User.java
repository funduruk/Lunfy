package ru.funduruk.meetgridServer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.util.UUID;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    private String username;
    private String email;
    private String passwordHash;
    private String avatarUrl;
    @CreationTimestamp
    private LocalDate createdDate;

    @OneToMany(mappedBy = "user")
    private Set<ServerMember> serverMemberships;

    @OneToMany(mappedBy = "owner")
    private Set<Server> ownedServers;
}

