package ru.funduruk.meetgridServer.entity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.funduruk.meetgridServer.entity.Server;
import ru.funduruk.meetgridServer.entity.ServerMember;
import ru.funduruk.meetgridServer.entity.User;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServerMemberRepository extends JpaRepository<ServerMember, UUID> {
    Optional<ServerMember> findByServerAndUser(Server server, User user);

}
