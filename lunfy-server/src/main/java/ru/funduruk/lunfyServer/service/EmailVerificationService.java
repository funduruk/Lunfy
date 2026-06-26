package ru.funduruk.lunfyServer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.funduruk.lunfyServer.entity.EmailVerification;
import ru.funduruk.lunfyServer.repository.EmailVerificationRepository;
import ru.funduruk.lunfyServer.util.CodeGenerator;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository repository;
    private final MailService mailService;

    private static final int CODE_TTL_MINUTES = 10;

    /** Создать и отправить код на почту */
    public void issueAndSend(String email) {
        String code = CodeGenerator.sixDigit();

        EmailVerification ev = new EmailVerification();
        ev.setEmail(email);
        ev.setCode(code);
        ev.setCreatedAt(LocalDateTime.now());
        ev.setExpiresAt(LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES));
        ev.setUsed(false);
        repository.save(ev);

        mailService.sendVerificationCode(email, code);
    }

    /** Проверить код. true если корректный и не просроченный */
    public boolean verify(String email, String code) {
        Optional<EmailVerification> opt =
                repository.findFirstByEmailAndUsedFalseOrderByCreatedAtDesc(email);

        if (opt.isEmpty()) return false;
        EmailVerification ev = opt.get();

        if (ev.getExpiresAt().isBefore(LocalDateTime.now())) return false;
        if (!ev.getCode().equals(code)) return false;

        ev.setUsed(true);
        repository.save(ev);
        return true;
    }
}