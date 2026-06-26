package ru.funduruk.lunfyServer.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import ru.funduruk.lunfyServer.entity.User;
import ru.funduruk.lunfyServer.service.EmailVerificationService;
import ru.funduruk.lunfyServer.service.JwtService;
import ru.funduruk.lunfyServer.service.PasswordResetService;
import ru.funduruk.lunfyServer.service.UserService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        try {
            String tag = String.format("%04d", (int)(Math.random() * 10000));

            User user = userService.register(
                    body.get("username"),
                    tag,
                    body.get("email"),
                    body.get("password")
            );

            emailVerificationService.issueAndSend(user.getEmail());

            return ResponseEntity.ok(Map.of(
                    "status", "verification_required",
                    "email", user.getEmail(),
                    "username", user.getUsername(),
                    "tag", user.getTag()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            String code = body.get("code");

            if (!emailVerificationService.verify(email, code)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Неверный или просроченный код"));
            }

            User user = userService.findByEmail(email);
            userService.markEmailVerified(user);

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

    @PostMapping("/resend")
    public ResponseEntity<?> resend(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            User user = userService.findByEmail(email);

            if (user.isEmailVerified()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Почта уже подтверждена"));
            }

            emailVerificationService.issueAndSend(email);
            return ResponseEntity.ok(Map.of("status", "sent"));
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

            if (!user.isEmailVerified()) {
                return ResponseEntity.status(403).body(Map.of(
                        "error", "Почта не подтверждена",
                        "status", "verification_required",
                        "email", user.getEmail()
                ));
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

    private final PasswordResetService passwordResetService;

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email не указан"));
            }

            try {
                userService.findByEmail(email);
            } catch (RuntimeException e) {
                return ResponseEntity.ok(Map.of("status", "sent"));
            }

            passwordResetService.issueAndSend(email);
            return ResponseEntity.ok(Map.of("status", "sent"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            String code = body.get("code");
            String newPassword = body.get("password");

            if (email == null || code == null || newPassword == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Не все поля заполнены"));
            }

            if (!passwordResetService.verify(email, code)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Неверный или просроченный код"));
            }

            User user = userService.findByEmail(email);
            userService.updatePassword(user, newPassword);

            return ResponseEntity.ok(Map.of("status", "password_changed"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}