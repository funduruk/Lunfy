package ru.funduruk.lunfyServer.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.funduruk.lunfyServer.entity.Message;
import ru.funduruk.lunfyServer.service.MessageService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final MessageService messageService;

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<?> getHistory(@PathVariable String chatId) {
        List<Message> messages = messageService.getHistory(chatId);

        List<Map<String, Object>> result = messages.stream()
                .filter(m -> !m.isDeleted())
                .map(m -> Map.<String, Object>of(
                        "id", m.getId(),
                        "sender", m.getSender().getUsername(),
                        "text", m.getText(),
                        "timestamp", m.getTimestamp().toString()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<?> deleteMessage(@PathVariable Long messageId) {
        messageService.deleteMessage(messageId);
        return ResponseEntity.ok(Map.of("message", "Сообщение удалено"));
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<?> deleteChat(@PathVariable String chatId) {
        try {
            messageService.deleteByChatId(chatId);
            return ResponseEntity.ok(Map.of("message", "Чат удалён"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}