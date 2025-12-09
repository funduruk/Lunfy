package ru.funduruk.Service;

import io.netty.channel.Channel;
import ru.funduruk.Entity.VoiceParticipant;
import ru.funduruk.Entity.VoiceSession;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class VoiceSessionService {

    private static final Map<UUID, VoiceSession> sessions = new HashMap<>();

    public VoiceSession getOrCreateSession(UUID sessionId) {
        return sessions.computeIfAbsent(sessionId, k -> new VoiceSession());
    }

    public void joinSession(UUID sessionId, UUID userId, Channel channel) {
        VoiceSession session = getOrCreateSession(sessionId);
        boolean exists = session.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(userId));
        if (exists) return;

        VoiceParticipant participant = new VoiceParticipant();
        participant.setUserId(userId);
        participant.setChannel(channel);
        session.getParticipants().add(participant);
    }

    public void leaveSession(UUID sessionId, UUID userId) {
        Optional.ofNullable(sessions.get(sessionId)).ifPresent(session -> {
            session.getParticipants().removeIf(p -> p.getUserId().equals(userId));
            if (session.getParticipants().isEmpty()) {
                sessions.remove(sessionId); // удаляем пустую сессию
            }
        });
    }

    public void toggleScreenShare(UUID sessionId, UUID userId) {
        Optional.ofNullable(sessions.get(sessionId)).ifPresent(session -> {
            session.getParticipants().stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .findFirst()
                    .ifPresent(p -> p.setSharingScreen(!p.isSharingScreen()));
        });
    }

    public static VoiceSession getSession(UUID sessionId) {
        return sessions.get(sessionId);
    }
}
