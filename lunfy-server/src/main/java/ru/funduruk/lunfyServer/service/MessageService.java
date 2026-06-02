package ru.funduruk.lunfyServer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.funduruk.lunfyServer.entity.Message;
import ru.funduruk.lunfyServer.entity.User;
import ru.funduruk.lunfyServer.repository.MessageRepository;
import ru.funduruk.lunfyServer.util.AESUtil;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;

    public Message save(User sender, String chatId, String text) {
        Message message = new Message();
        message.setSender(sender);
        message.setChatId(chatId);
        message.setText(AESUtil.encrypt(text)); // шифруем перед сохранением
        return messageRepository.save(message);
    }

    public List<Message> getHistory(String chatId) {
        List<Message> messages = messageRepository.findByChatIdOrderByTimestampAsc(chatId);
        // расшифровываем перед отдачей
        messages.forEach(m -> m.setText(AESUtil.decrypt(m.getText())));
        return messages;
    }

    public void deleteMessage(Long messageId) {
        messageRepository.findById(messageId).ifPresent(m -> {
            m.setDeleted(true);
            messageRepository.save(m);
        });
    }

    public void deleteByChatId(String chatId) {
        messageRepository.deleteByChatId(chatId);
    }
}