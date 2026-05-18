package ru.funduruk.lunfyServer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.funduruk.lunfyServer.entity.Friendship;
import ru.funduruk.lunfyServer.entity.User;
import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    List<Friendship> findByReceiverAndStatus(User receiver, Friendship.FriendshipStatus status);
    List<Friendship> findBySenderAndStatus(User sender, Friendship.FriendshipStatus status);
    Optional<Friendship> findBySenderAndReceiver(User sender, User receiver);

}