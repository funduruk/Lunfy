package ru.funduruk.meetgridServer.service.entity;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.funduruk.meetgridServer.entity.Channel;
import ru.funduruk.meetgridServer.entity.User;
import ru.funduruk.meetgridServer.entity.VoiceParticipant;
import ru.funduruk.meetgridServer.entity.VoiceSession;
import ru.funduruk.meetgridServer.entity.enums.ChannelType;
import ru.funduruk.meetgridServer.entity.repository.VoiceSessionRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;

@Service
@AllArgsConstructor
public class VoiceSessionService {
    private final VoiceSessionRepository voiceSessionRepository;

    public void joinInVoiceSession(User user, Channel channel) {
        if (!channel.getType().equals(ChannelType.VOICE)) {
            throw new IllegalArgumentException("This is not a voice channel");
        }

        VoiceSession voiceSession = voiceSessionRepository.findActiveByChannel(channel)
                .orElseGet(() -> {
                    VoiceSession newSession = new VoiceSession();
                    newSession.setChannel(channel);
                    newSession.setStartedAt(LocalDateTime.now());
                    newSession.setParticipants(new HashSet<>());
                    return newSession;
                });

        boolean alreadyInSession = voiceSession.getParticipants().stream()
                .anyMatch(p -> p.getUser().equals(user));
        if (alreadyInSession) return;

        VoiceParticipant participant = new VoiceParticipant();
        participant.setUser(user);
        participant.setSession(voiceSession);
        voiceSession.getParticipants().add(participant);

        voiceSessionRepository.save(voiceSession);
    }

    public void leaveVoiceSession(User user, Channel channel) {
        if (!channel.getType().equals(ChannelType.VOICE)) {
            throw new IllegalArgumentException("This is not a voice channel");
        }

        Optional<VoiceSession> sessionOpt = voiceSessionRepository.findActiveByChannel(channel);
        if (sessionOpt.isEmpty()) return; // сессии нет, ничего делать не нужно

        VoiceSession session = sessionOpt.get();

        Optional<VoiceParticipant> participantOpt = session.getParticipants().stream()
                .filter(p -> p.getUser().equals(user))
                .findFirst();

        if (participantOpt.isPresent()) {
            session.getParticipants().remove(participantOpt.get());
        }

        if (session.getParticipants().isEmpty()) {
            session.setEndedAt(LocalDateTime.now());
        }
        voiceSessionRepository.save(session);
    }

    public void toggleScreenShare(User user, Channel channel) {
        if (!channel.getType().equals(ChannelType.VOICE)) {
            throw new IllegalArgumentException("This is not a voice channel");
        }

        Optional<VoiceSession> sessionOpt = voiceSessionRepository.findActiveByChannel(channel);
        if (sessionOpt.isEmpty()) return;

        VoiceSession session = sessionOpt.get();

        session.getParticipants().stream()
                .filter(p -> p.getUser().equals(user))
                .findFirst()
                .ifPresent(p -> {
                    p.setSharingScreen(!p.isSharingScreen());
                    voiceSessionRepository.save(session);
                });
    }

}
