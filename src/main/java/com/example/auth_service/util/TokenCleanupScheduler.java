package com.example.auth_service.util;

import com.example.auth_service.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final RefreshTokenRepository repository;

    @Scheduled(fixedRate = 600000)
    @Transactional
    public void deleteExpiredTokens() {

        int deleted = repository.deleteAllExpired(LocalDateTime.now());

        log.info(
                "{} expired refresh tokens removed",
                deleted
        );
    }
}
