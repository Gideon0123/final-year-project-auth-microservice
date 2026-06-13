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
public class UserStatusScheduler {

    private final UserRepository userRepository;

    @Scheduled(fixedRate = 600000)
    @Transactional
    public void reactivateSuspendedUsers() {

        List<User> users =
                userRepository
                        .findByStatusAndSuspendedUntilBefore(
                                AccountStatus.SUSPENDED,
                                LocalDateTime.now()
                        );

        users.forEach(user -> {

            user.setStatus(
                    AccountStatus.ACTIVE
            );

            user.setSuspendedUntil(null);
        });
    }
}