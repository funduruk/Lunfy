package ru.funduruk.meetgridServer.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import ru.funduruk.meetgridServer.dto.EnvelopeDTO;

@Controller
public class ChatController {

    @MessageMapping("/chat") // клиент отправляет на /app/chat
    @SendTo("/topic/messages") // сервер рассылает всем подписанным
    public EnvelopeDTO sendMessage(EnvelopeDTO envelope) {
        // пока просто возвращаем сообщение обратно всем
        return envelope;
    }
}