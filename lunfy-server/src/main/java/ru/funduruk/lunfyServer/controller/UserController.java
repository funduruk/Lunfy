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

    @PostMapping("/me/avatar")
    public ResponseEntity<?> uploadAvatar(@AuthenticationPrincipal String username,
                                          @RequestParam("file")
                                          org.springframework.web.multipart.MultipartFile file) {
        try {
            User user = userService.findByUsername(username);
            String fileName = user.getId() + "_" + System.currentTimeMillis() +
                    getExtension(file.getOriginalFilename());
            java.nio.file.Path path = java.nio.file.Paths.get("uploads/users/" + fileName);
            java.nio.file.Files.createDirectories(path.getParent());
            java.nio.file.Files.write(path, file.getBytes());

            user.setAvatarPath(path.toString());
            userService.save(user);

            return ResponseEntity.ok(Map.of("avatarPath", path.toString()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{username}/avatar")
    public ResponseEntity<byte[]> getAvatar(@PathVariable String username) {
        try {
            User user = userService.findByUsername(username);
            if (user.getAvatarPath() == null) {
                return ResponseEntity.notFound().build();
            }

            java.nio.file.Path path = java.nio.file.Paths.get(user.getAvatarPath());
            if (!java.nio.file.Files.exists(path)) {
                return ResponseEntity.notFound().build();
            }

            byte[] bytes = java.nio.file.Files.readAllBytes(path);
            String contentType = "image/png";
            String name = path.getFileName().toString().toLowerCase();
            if (name.endsWith(".jpg") || name.endsWith(".jpeg")) contentType = "image/jpeg";

            return ResponseEntity.ok()
                    .header("Content-Type", contentType)
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return ".png";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".png";
    }
}