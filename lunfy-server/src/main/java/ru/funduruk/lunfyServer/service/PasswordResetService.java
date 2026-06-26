package ru.funduruk.lunfyServer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.funduruk.lunfyServer.entity.PasswordReset;
import ru.funduruk.lunfyServer.repository.PasswordResetRepository;
import ru.funduruk.lunfyServer.util.CodeGenerator;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetRepository repository;
    private final MailService mailService;

    private static final int CODE_TTL_MINUTES = 10;

    public void issueAndSend(String email) {
        String code = CodeGenerator.sixDigit();

        PasswordReset pr = new PasswordReset();
        pr.setEmail(email);
        pr.setCode(code);
        pr.setCreatedAt(LocalDateTime.now());
        pr.setExpiresAt(LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES));
        pr.setUsed(false);
        repository.save(pr);

        mailService.sendPasswordResetCode(email, code);
    }

    public boolean verify(String email, String code) {
        Optional<PasswordReset> opt =
                repository.findFirstByEmailAndUsedFalseOrderByCreatedAtDesc(email);

        if (opt.isEmpty()) return false;
        PasswordReset pr = opt.get();

        if (pr.getExpiresAt().isBefore(LocalDateTime.now())) return false;
        if (!pr.getCode().equals(code)) return false;

        pr.setUsed(true);
        repository.save(pr);
        return true;
    }
}