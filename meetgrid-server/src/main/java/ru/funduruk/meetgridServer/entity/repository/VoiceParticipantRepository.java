package ru.funduruk.meetgridServer.entity.repository;

import ru.funduruk.meetgridServer.entity.User;
import ru.funduruk.meetgridServer.entity.VoiceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.funduruk.meetgridServer.entity.VoiceParticipant;

import java.util.List;
import java.util.UUID;

@Repository
public interface VoiceParticipantRepository extends JpaRepository<VoiceParticipant, UUID> {
    List<VoiceParticipant> findBySession(VoiceSession session);

    List<VoiceParticipant> findByUser(User user);
}
