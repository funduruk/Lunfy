package ru.funduruk.meetgridServer.entity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.funduruk.meetgridServer.entity.Channel;
import ru.funduruk.meetgridServer.entity.Server;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, UUID> {
    List<Channel> findByServer(Server server);

    Optional<Channel> findByServerAndName(Server server, String name);
}
