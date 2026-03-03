package ru.funduruk.lunfyServer.ws;

import lombok.RequiredArgsConstructor;

import org.funduruk.dto.ConnectDTO;
import org.funduruk.dto.EnvelopeDTO;
import org.funduruk.dto.MessageDTO;
import org.funduruk.dto.TypingDTO;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper mapper = new ObjectMapper();
    private final SessionRegistry registry = new SessionRegistry();



    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("Connected: " + session.getId());
        registry.add(session.getId(), session);
    }


    // В ChatWebSocketHandler добавь:
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        registry.remove(session.getId());
        System.out.println("Removed session: " + session.getId());
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        EnvelopeDTO env = mapper.readValue(message.getPayload(), EnvelopeDTO.class);

        switch (env.getType()) {

            case "CONNECT" -> {
                ConnectDTO dto =
                        mapper.convertValue(env.getData(), ConnectDTO.class);
                System.out.println("CONNECT: " + dto.getUserId());
            }

            case "CHAT_MESSAGE" -> {
                MessageDTO dto =
                        mapper.convertValue(env.getData(), MessageDTO.class);
                System.out.println("MSG: " + dto.getText());
                broadcast(env);
            }
        }
    }

    private void handleConnect(WebSocketSession session, EnvelopeDTO env) {
        ConnectDTO dto = mapper.convertValue(env.getData(), ConnectDTO.class);
        registry.add(dto.getUserId(), session);
        System.out.println("User connected: " + dto.getUserId());
    }

    private void broadcast(EnvelopeDTO env) throws Exception {
        String json = mapper.writeValueAsString(env);
        System.out.println("BROADCASTING TO " + registry.all().size() + " sessions: " + json);
        for (WebSocketSession s : registry.all()) {
            System.out.println("Session open: " + s.isOpen() + " id: " + s.getId());
            if (s.isOpen()) {
                s.sendMessage(new TextMessage(json));
            }
        }
    }

    private void handleTyping(WebSocketSession session, EnvelopeDTO env) throws Exception {
        TypingDTO dto = mapper.convertValue(env.getData(), TypingDTO.class);

        for (WebSocketSession s : registry.all()) {
            if (!s.getId().equals(session.getId())) {
                s.sendMessage(new TextMessage(
                        mapper.writeValueAsString(env)
                ));
            }
        }
    }
}