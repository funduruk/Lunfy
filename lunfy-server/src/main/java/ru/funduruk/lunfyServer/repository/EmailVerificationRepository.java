package ru.funduruk.lunfyServer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.funduruk.lunfyServer.entity.EmailVerification;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findFirstByEmailAndUsedFalseOrderByCreatedAtDesc(String email);
}