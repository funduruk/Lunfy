package ru.funduruk.lunfyServer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.funduruk.lunfyServer.entity.User;
import ru.funduruk.lunfyServer.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User register(String username, String tag, String email, String password) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new RuntimeException("Имя пользователя уже занято");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new RuntimeException("Email уже используется");
        }

        User user = new User();
        user.setUsername(username);
        user.setTag(tag);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEmailVerified(false);

        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Пользователь с такой почтой не найден"));
    }

    public void markEmailVerified(User user) {
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

    public User findByUsernameAndTag(String username, String tag) {
        return userRepository.findByUsernameAndTag(username, tag)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

    public boolean checkPassword(User user, String rawPassword, PasswordEncoder encoder) {
        return encoder.matches(rawPassword, user.getPasswordHash());
    }

    public void save(User user) {
        userRepository.save(user);
    }

    public void updatePassword(User user, String newPassword) {
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}