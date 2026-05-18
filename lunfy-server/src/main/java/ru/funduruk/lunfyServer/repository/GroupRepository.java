package ru.funduruk.lunfyServer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.funduruk.lunfyServer.entity.Group;

public interface GroupRepository extends JpaRepository<Group, Long> {
}