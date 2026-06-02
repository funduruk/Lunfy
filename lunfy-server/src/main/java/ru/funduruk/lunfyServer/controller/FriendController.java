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

    @PostMapping("/request")
    public ResponseEntity<?> sendRequest(@AuthenticationPrincipal String username,
                                         @RequestBody Map<String, String> body) {
        try {
            User sender = userService.findByUsername(username);
            User receiver = userService.findByUsernameAndTag(
                    body.get("username"), body.get("tag")
            );
            friendshipService.sendRequest(sender, receiver);
            return ResponseEntity.ok(Map.of("message", "Заявка отправлена"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<?> accept(@PathVariable Long id) {
        friendshipService.accept(id);
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
            friendshipService.remove(id);
            return ResponseEntity.ok(Map.of("message", "Друг удалён"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


}