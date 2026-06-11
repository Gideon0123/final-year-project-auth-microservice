package com.example.auth_service.util;


import com.example.auth_service.repository.PasswordResetTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class PasswordResetTokenCleanupScheduler {

    private final PasswordResetTokenRepository repository;

    @Scheduled(fixedRate = 600000)
    @Transactional
    public void deleteExpiredTokens() {
        repository.deleteByExpiryDateBefore(
                LocalDateTime.now()
        );
    }
}