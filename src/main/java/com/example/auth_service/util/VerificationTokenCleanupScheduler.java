package com.example.auth_service.util;

import com.example.auth_service.repository.EmailVerificationTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class VerificationTokenCleanupScheduler {

    private final EmailVerificationTokenRepository repository;

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanup() {

        int count = repository.deleteExpired(
                LocalDateTime.now()
        );

        log.info(
                "{} expired verification tokens deleted",
                count
        );
    }
}
