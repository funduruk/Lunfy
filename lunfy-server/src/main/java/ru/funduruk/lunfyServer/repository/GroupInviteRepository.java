package ru.funduruk.lunfyServer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.funduruk.lunfyServer.entity.GroupInvite;
import java.util.List;
import java.util.Optional;

public interface GroupInviteRepository extends JpaRepository<GroupInvite, Long> {
    List<GroupInvite> findByInvitedUserIdAndStatus(Long userId, GroupInvite.Status status);

    Optional<GroupInvite> findByGroupIdAndInvitedUserIdAndStatus(
            Long groupId, Long userId, GroupInvite.Status status);
}