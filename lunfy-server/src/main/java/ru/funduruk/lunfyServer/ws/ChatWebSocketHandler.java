package ru.funduruk.meetgridServer.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ru.funduruk.meetgridServer.dto.CallSignalDTO;
import ru.funduruk.meetgridServer.dto.ChatMessageDTO;
import ru.funduruk.meetgridServer.dto.EnvelopeDTO;
import tools.jackson.databind.ObjectMapper;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper mapper = new ObjectMapper();
    private final SessionRegistry sessions;

    public ChatWebSocketHandler(SessionRegistry sessions) {
        this.sessions = sessions;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("Connected: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message)
            throws Exception {

        EnvelopeDTO envelope = mapper.readValue(
                message.getPayload(), EnvelopeDTO.class
        );

        switch (envelope.getType()) {
            case "CONNECT" -> handleConnect(session, envelope);
            case "CHAT_MESSAGE" -> handleChatMessage(session, envelope);
            case "TYPING" -> handleTyping(session, envelope);
            case "CALL_OFFER", "CALL_ANSWER", "ICE_CANDIDATE" ->
                    handleCallSignal(envelope);
        }
    }

    private void handleConnect(WebSocketSession session, EnvelopeDTO env) {
        String userId = (String) env.getPayload();
        sessions.add(userId, session);
    }

    private void handleChatMessage(WebSocketSession session, EnvelopeDTO env)
            throws Exception {

        ChatMessageDTO msg = mapper.convertValue(
                env.getPayload(), ChatMessageDTO.class
        );

        // пока просто рассылаем всем (потом по chatId)
        broadcast(new EnvelopeDTO("CHAT_MESSAGE", msg));
    }

    private void handleTyping(WebSocketSession session, EnvelopeDTO env)
            throws Exception {
        broadcast(env);
    }

    private void handleCallSignal(EnvelopeDTO env) throws Exception {
        CallSignalDTO dto = mapper.convertValue(
                env.getPayload(), CallSignalDTO.class
        );

        WebSocketSession target = sessions.get(dto.getTo());
        if (target != null && target.isOpen()) {
            target.sendMessage(
                    new TextMessage(mapper.writeValueAsString(env))
            );
        }
    }

    private void broadcast(EnvelopeDTO env) throws Exception {
        String json = mapper.writeValueAsString(env);
        for (WebSocketSession s : sessions.all()) {
            if (s.isOpen()) {
                s.sendMessage(new TextMessage(json));
            }
        }
    }
}
