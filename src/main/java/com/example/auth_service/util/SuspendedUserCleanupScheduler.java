package com.example.auth_service.util;

import com.example.auth_service.entity.User;
import com.example.auth_service.enums.AccountStatus;
import com.example.auth_service.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SuspendedUserCleanupScheduler {

    private final UserRepository userRepository;

    @Scheduled(fixedRate = 600000)
    @Transactional
    public void reactivateExpiredSuspensions() {

        List<User> users =
                userRepository
                        .findByStatusAndSuspendedUntilBefore(
                                AccountStatus.SUSPENDED,
                                LocalDateTime.now()
                        );

        for (User user : users) {

            user.setStatus(
                    AccountStatus.ACTIVE
            );

            user.setSuspendedUntil(
                    null
            );
        }
    }
}