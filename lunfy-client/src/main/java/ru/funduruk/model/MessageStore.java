package ru.funduruk.model;

import org.funduruk.dto.MessageDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageStore {

    private static MessageStore instance;
    private final Map<String, List<MessageDTO>> messages = new HashMap<>();

    private MessageStore() {}

    public static MessageStore getInstance() {
        if (instance == null) instance = new MessageStore();
        return instance;
    }

    public List<MessageDTO> getMessages(String chatId) {
        return messages.getOrDefault(chatId, new ArrayList<>());
    }

    public void addMessage(String chatId, MessageDTO msg) {
        messages.computeIfAbsent(chatId, k -> new ArrayList<>()).add(msg);
    }

    public void ensureChat(String chatId) {
        messages.putIfAbsent(chatId, new ArrayList<>());
    }
}
