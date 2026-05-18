package ru.funduruk.lunfyServer.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import ru.funduruk.lunfyServer.entity.User;
import ru.funduruk.lunfyServer.service.JwtService;
import ru.funduruk.lunfyServer.service.UserService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        try {
            // Генерируем случайный 4-значный тег
            String tag = String.format("%04d", (int)(Math.random() * 10000));

            User user = userService.register(
                    body.get("username"),
                    tag,
                    body.get("email"),
                    body.get("password")
            );

            String token = jwtService.generateToken(user.getUsername());
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "username", user.getUsername(),
                    "tag", user.getTag()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            User user = userService.findByUsername(body.get("username"));

            if (!userService.checkPassword(user, body.get("password"), passwordEncoder)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Неверный пароль"));
            }

            user.setStatus(User.UserStatus.ONLINE);
            userService.save(user);

            String token = jwtService.generateToken(user.getUsername());
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "username", user.getUsername(),
                    "tag", user.getTag(),
                    "id", user.getId()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}