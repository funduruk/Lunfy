package ru.funduruk.meetgridServer.service.entity;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.funduruk.meetgridServer.entity.Channel;
import ru.funduruk.meetgridServer.entity.Message;
import ru.funduruk.meetgridServer.entity.User;
import ru.funduruk.meetgridServer.entity.repository.MessageRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;

    public Message sendMessage(Message message, Channel channel, User user) {
        Message newMessage = new Message();

        newMessage.setSender(user);
        newMessage.setChannel(channel);
        newMessage.setContent(message.getContent());
        newMessage.setCreatedAt(LocalDateTime.now());

        return messageRepository.save(newMessage);
    }

    public List<Message> getAllMessages(Channel channel) {
        return messageRepository.findByChannelOrderByCreatedAtAsc(channel);
    }

    public Message changeMessage(Message message, Channel channel, User user, String content) {
        if(channel.equals(message.getChannel()) && user.equals(message.getSender())) {
            message.setContent(content);
            return messageRepository.save(message);
        } else {
            throw new IllegalArgumentException("You are not allowed to change the message");
        }
    }

    public void deleteMessage(Message message, Channel channel, User user) {
        if(channel.equals(message.getChannel()) && user.equals(message.getSender())) {
            messageRepository.delete(message);
        } else {
            throw new IllegalArgumentException("You are not allowed to delete the message");
        }
    }
}
