package ru.funduruk.lunfyServer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.funduruk.lunfyServer.entity.Friendship;
import ru.funduruk.lunfyServer.entity.User;
import ru.funduruk.lunfyServer.repository.FriendshipRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;

    public void sendRequest(User sender, User receiver) {
        friendshipRepository.findBySenderAndReceiver(sender, receiver)
                .ifPresent(f -> { throw new RuntimeException("Заявка уже отправлена"); });

        Friendship friendship = new Friendship();
        friendship.setSender(sender);
        friendship.setReceiver(receiver);
        friendshipRepository.save(friendship);
    }

    public Friendship accept(Long friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));
        friendship.setStatus(Friendship.FriendshipStatus.ACCEPTED);
        return friendshipRepository.save(friendship);
    }

    public Friendship findById(Long friendshipId) {
        return friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));
    }

    public void decline(Long friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));
        friendship.setStatus(Friendship.FriendshipStatus.DECLINED);
        friendshipRepository.save(friendship);
    }

    public List<Friendship> getIncoming(User user) {
        return friendshipRepository.findByReceiverAndStatus(user, Friendship.FriendshipStatus.PENDING);
    }

    public List<Friendship> getFriends(User user) {
        List<Friendship> asSender = friendshipRepository.findBySenderAndStatus(user, Friendship.FriendshipStatus.ACCEPTED);
        List<Friendship> asReceiver = friendshipRepository.findByReceiverAndStatus(user, Friendship.FriendshipStatus.ACCEPTED);

        List<Friendship> all = new ArrayList<>();
        all.addAll(asSender);
        all.addAll(asReceiver);
        return all;
    }

    public void remove(Long friendshipId) {
        friendshipRepository.deleteById(friendshipId);
    }
}