package ru.funduruk.meetgridServer.entity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.funduruk.meetgridServer.entity.Server;
import ru.funduruk.meetgridServer.entity.User;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServerRepository extends JpaRepository<Server, UUID> {
   List<Server> findByOwner(User owner);
}
