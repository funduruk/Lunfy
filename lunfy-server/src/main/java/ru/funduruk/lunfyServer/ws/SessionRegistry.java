package ru.funduruk.meetgridServer.ws;

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

    public void remove(String userId) {
        sessions.remove(userId);
    }

    public WebSocketSession get(String userId) {
        return sessions.get(userId);
    }

    public Collection<WebSocketSession> all() {
        return sessions.values();
    }
}
