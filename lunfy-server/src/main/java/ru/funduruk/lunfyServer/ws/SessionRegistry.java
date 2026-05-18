package ru.funduruk.lunfyServer.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionRegistry {
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void add(String username, WebSocketSession session) {
        sessions.put(username, session);
    }

    public void remove(String username) {
        sessions.values().removeIf(s -> s.getId().equals(username));
    }

    public Collection<WebSocketSession> all() {
        return sessions.values();
    }

    public WebSocketSession getByUsername(String username) {
        return sessions.get(username);
    }
}