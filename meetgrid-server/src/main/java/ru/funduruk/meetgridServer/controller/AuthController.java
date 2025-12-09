package ru.funduruk.meetgridServer.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.funduruk.meetgridServer.service.JwtService;
import ru.funduruk.meetgridServer.service.dto.AuthResponse;
import ru.funduruk.meetgridServer.service.dto.LoginRequest;
import ru.funduruk.meetgridServer.service.dto.RegisterRequest;
import ru.funduruk.meetgridServer.service.entity.UserService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        AuthResponse response = userService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate")
    public boolean validate(@RequestBody String token) {
        try {
            jwtService.validate(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}


