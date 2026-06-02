package ru.funduruk.lunfyServer.repository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import ru.funduruk.lunfyServer.entity.Message;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChatIdOrderByTimestampAsc(String chatId);

    @Modifying
    @Transactional
    void deleteByChatId(String chatId);

}