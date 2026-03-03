package ru.funduruk.lunfyServer.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionRegistry {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void add(String userId, WebSocketSession session) {
        sessions.put(userId, session);
    }

    public Collection<WebSocketSession> all() {
        return sessions.values();
    }

    public void remove(String userId) { sessions.values().removeIf(s -> s.getId().equals(userId)); }
}