package ru.funduruk.lunfyServer.ws;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.funduruk.dto.*;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ru.funduruk.lunfyServer.entity.User;
import ru.funduruk.lunfyServer.repository.UserRepository;
import ru.funduruk.lunfyServer.service.MessageService;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    public void sendToUserByUsername(String username, EnvelopeDTO env) throws Exception {
        sendToUser(username, env);
    }

    private final SessionRegistry registry = new SessionRegistry();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String username = getUsername(session);
        if (username != null) {
            registry.add(username, session);
            System.out.println("Connected: " + username);
        }
    }

    private String getUsername(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=");
            if (kv.length == 2 && kv[0].equals("username")) return kv[1];
        }
        return null;
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        registry.removeBySession(session);
    }

    private final ObjectMapper mapper = new ObjectMapper();

    private final MessageService messageService;
    private final UserRepository userRepository;

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
                String dataJson = mapper.writeValueAsString(env.getData());
                MessageDTO dto = mapper.readValue(dataJson, MessageDTO.class);

                User sender = userRepository.findByUsername(dto.getSender()).orElse(null);
                if (sender != null) {
                    messageService.save(sender, dto.getChatId(), dto.getText());
                }

                String chatId = dto.getChatId();

                if (chatId.startsWith("dm-")) {
                    String[] parts = chatId.replace("dm-", "").split("-");
                    if (parts.length == 2) {
                        sendToUser(parts[0], env);
                        sendToUser(parts[1], env);
                    }
                } else {
                    broadcast(env);
                }
            }

            case "DELETE_MESSAGE" -> {
                String dataJson = mapper.writeValueAsString(env.getData());
                MessageDTO dto = mapper.readValue(dataJson, MessageDTO.class);

                // Удаляем из БД
                messageService.deleteMessage(dto.getId());

                // Рассылаем участникам чата
                String chatId = dto.getChatId();
                if (chatId.startsWith("dm-")) {
                    String[] parts = chatId.replace("dm-", "").split("-");
                    if (parts.length == 2) {
                        sendToUser(parts[0], env);
                        sendToUser(parts[1], env);
                    }
                } else {
                    broadcast(env);
                }
            }

            case "DELETE_CHAT" -> {
                String dataJson = mapper.writeValueAsString(env.getData());
                MessageDTO dto = mapper.readValue(dataJson, MessageDTO.class);
                String chatId = dto.getChatId();

                messageService.deleteByChatId(chatId);

                if (chatId.startsWith("dm-")) {
                    String[] parts = chatId.replace("dm-", "").split("-");
                    if (parts.length == 2) {
                        sendToUser(parts[0], env);
                        sendToUser(parts[1], env);
                    }
                } else {
                    broadcast(env);
                }
            }

            case "CALL_OFFER", "CALL_ANSWER", "CALL_REJECT", "CALL_END" -> {
                String dataJson = mapper.writeValueAsString(env.getData());
                CallSignalDTO signal = mapper.readValue(dataJson, CallSignalDTO.class);

                System.out.println("CALL signal " + env.getType() +
                        " from " + signal.getFromUser() + " to " + signal.getToUser());

                sendToUserByUsername(signal.getToUser(), env);
            }

            case "AUDIO_CHUNK" -> {
                String dataJson = mapper.writeValueAsString(env.getData());
                AudioChunkDTO chunk = mapper.readValue(dataJson, AudioChunkDTO.class);
                sendToUserByUsername(chunk.getToUser(), env);
            }

            case "SCREEN_FRAME", "SCREEN_SHARE_START", "SCREEN_SHARE_STOP" -> {
                String dataJson = mapper.writeValueAsString(env.getData());
                ScreenFrameDTO frame = mapper.readValue(dataJson, ScreenFrameDTO.class);
                sendToUserByUsername(frame.getToUser(), env);
            }

            case "VIDEO_FRAME", "VIDEO_START", "VIDEO_STOP" -> {
                String dataJson = mapper.writeValueAsString(env.getData());
                VideoFrameDTO frame = mapper.readValue(dataJson, VideoFrameDTO.class);
                sendToUserByUsername(frame.getToUser(), env);
            }
        }
    }

    private void sendToUser(String username, EnvelopeDTO env) throws Exception {
        WebSocketSession session = registry.getByUsername(username);
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(mapper.writeValueAsString(env)));
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

    //TODO
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