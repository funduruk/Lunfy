package ru.funduruk.meetgridServer.service.entity;

import lombok.AllArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import ru.funduruk.meetgridServer.entity.User;
import ru.funduruk.meetgridServer.entity.repository.UserRepository;
import ru.funduruk.meetgridServer.service.JwtService;
import ru.funduruk.meetgridServer.service.dto.AuthResponse;
import ru.funduruk.meetgridServer.service.dto.LoginRequest;
import ru.funduruk.meetgridServer.service.dto.RegisterRequest;

import java.util.Optional;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setUsername(request.username());
        user.setPasswordHash(bCryptPasswordEncoder.encode(request.password()));
        userRepository.save(user);

        String token = jwtService.generateAccessToken(user);
        return new AuthResponse(user.getId().toString(), token);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!bCryptPasswordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect password");
        }

        String token = jwtService.generateAccessToken(user);
        return new AuthResponse(user.getId().toString(), token);
    }


    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User updateUser(User user) {
        if (user.getId() == null || !userRepository.existsById(user.getId())) {
            throw new IllegalArgumentException("User not found");
        }
        return userRepository.save(user);
    }


}
