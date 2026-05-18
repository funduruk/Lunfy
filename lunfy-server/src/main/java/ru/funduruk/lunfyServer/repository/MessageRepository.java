package ru.funduruk.lunfyServer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.funduruk.lunfyServer.entity.Message;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChatIdOrderByTimestampAsc(String chatId);
}