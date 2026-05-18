package ru.funduruk.lunfyServer.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.funduruk.lunfyServer.entity.User;
import ru.funduruk.lunfyServer.service.UserService;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<?> getMe(@AuthenticationPrincipal String username) {
        User user = userService.findByUsername(username);
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "tag", user.getTag(),
                "bio", user.getBio() != null ? user.getBio() : "",
                "status", user.getStatus().name(),
                "avatarPath", user.getAvatarPath() != null ? user.getAvatarPath() : ""
        ));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMe(@AuthenticationPrincipal String username,
                                      @RequestBody Map<String, String> body) {
        User user = userService.findByUsername(username);

        if (body.containsKey("bio")) user.setBio(body.get("bio"));
        if (body.containsKey("status")) {
            try {
                user.setStatus(User.UserStatus.valueOf(body.get("status")));
            } catch (IllegalArgumentException ignored) {}
        }

        userService.save(user);
        return ResponseEntity.ok(Map.of("message", "Профиль обновлён"));
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String username,
                                    @RequestParam String tag) {
        try {
            User user = userService.findByUsernameAndTag(username, tag);
            return ResponseEntity.ok(Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "tag", user.getTag()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}