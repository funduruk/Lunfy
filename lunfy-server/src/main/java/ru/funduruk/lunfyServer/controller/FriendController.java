package ru.funduruk.lunfyServer.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.funduruk.lunfyServer.entity.Friendship;
import ru.funduruk.lunfyServer.entity.User;
import ru.funduruk.lunfyServer.service.FriendshipService;
import ru.funduruk.lunfyServer.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendshipService friendshipService;
    private final UserService userService;
    private final ru.funduruk.lunfyServer.ws.ChatWebSocketHandler ws;

    @PostMapping("/request")
    public ResponseEntity<?> sendRequest(@AuthenticationPrincipal String username,
                                         @RequestBody Map<String, String> body) {
        try {
            User sender = userService.findByUsername(username);
            User receiver = userService.findByUsernameAndTag(
                    body.get("username"), body.get("tag"));
            friendshipService.sendRequest(sender, receiver);

            safeSend(receiver.getUsername(), new org.funduruk.dto.EnvelopeDTO("FRIEND_REQUEST",
                    Map.of("from", sender.getUsername(), "tag", sender.getTag())));

            return ResponseEntity.ok(Map.of("message", "Заявка отправлена"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<?> accept(@PathVariable Long id) {
        Friendship f = friendshipService.accept(id);

        if (f != null) {
            String u1 = f.getSender().getUsername();
            String u2 = f.getReceiver().getUsername();
            org.funduruk.dto.EnvelopeDTO env = new org.funduruk.dto.EnvelopeDTO(
                    "FRIEND_ACCEPTED", Map.of("user1", u1, "user2", u2));

            safeSend(u1, env);
            safeSend(u2, env);
        }
        return ResponseEntity.ok(Map.of("message", "Заявка принята"));
    }

    @PostMapping("/{id}/decline")
    public ResponseEntity<?> decline(@PathVariable Long id) {
        friendshipService.decline(id);
        return ResponseEntity.ok(Map.of("message", "Заявка отклонена"));
    }

    @GetMapping("/incoming")
    public ResponseEntity<?> incoming(@AuthenticationPrincipal String username) {
        User user = userService.findByUsername(username);
        List<Friendship> incoming = friendshipService.getIncoming(user);

        List<Map<String, Object>> result = incoming.stream()
                .map(f -> Map.<String, Object>of(
                        "id", f.getId(),
                        "from", f.getSender().getUsername(),
                        "tag", f.getSender().getTag()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<?> getFriends(@AuthenticationPrincipal String username) {
        User user = userService.findByUsername(username);
        List<Friendship> friends = friendshipService.getFriends(user);

        List<Map<String, Object>> result = friends.stream()
                .map(f -> {
                    User friend = f.getSender().getUsername().equals(username)
                            ? f.getReceiver()
                            : f.getSender();

                    return Map.<String, Object>of(
                            "id", f.getId(),
                            "username", friend.getUsername(),
                            "tag", friend.getTag(),
                            "status", friend.getStatus().name()
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> removeFriend(@PathVariable Long id) {
        try {
            Friendship f = friendshipService.findById(id); // нужен метод, см ниже
            String u1 = f.getSender().getUsername();
            String u2 = f.getReceiver().getUsername();

            friendshipService.remove(id);

            org.funduruk.dto.EnvelopeDTO env = new org.funduruk.dto.EnvelopeDTO(
                    "FRIEND_REMOVED", Map.of("user1", u1, "user2", u2));
            ws.sendToUserByUsername(u1, env);
            ws.sendToUserByUsername(u2, env);

            return ResponseEntity.ok(Map.of("message", "Друг удалён"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private void safeSend(String username, org.funduruk.dto.EnvelopeDTO env) {
        try {
            ws.sendToUserByUsername(username, env);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}