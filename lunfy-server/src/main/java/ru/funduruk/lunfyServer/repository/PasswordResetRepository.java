package ru.funduruk.lunfyServer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.funduruk.lunfyServer.entity.PasswordReset;

import java.util.Optional;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {
    Optional<PasswordReset> findFirstByEmailAndUsedFalseOrderByCreatedAtDesc(String email);
}