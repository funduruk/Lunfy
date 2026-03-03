package ru.funduruk.lunfyServer.controller;

import org.funduruk.dto.EnvelopeDTO;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    @MessageMapping("/chat")
    @SendTo("/topic/messages")
    public EnvelopeDTO sendMessage(EnvelopeDTO envelope) {
        return envelope;
    }
}