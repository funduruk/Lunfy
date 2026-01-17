package ru.funduruk.meetgridServer.entity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.funduruk.meetgridServer.entity.Channel;
import ru.funduruk.meetgridServer.entity.VoiceSession;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoiceSessionRepository extends JpaRepository<VoiceSession, UUID> {
    Optional<VoiceSession> findActiveByChannel(Channel channel);
}
