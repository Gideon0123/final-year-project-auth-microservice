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
@Slf4j
public class PasswordTokenResetCleanupScheduler {

    PasswordResetTokenRepository passwordResetTokenRepository;

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanup() {

        int count = passwordResetTokenRepository.deleteExpired(
                LocalDateTime.now()
        );

        log.info(
                "{} expired Password Reset tokens deleted",
                count
        );
    }
}
