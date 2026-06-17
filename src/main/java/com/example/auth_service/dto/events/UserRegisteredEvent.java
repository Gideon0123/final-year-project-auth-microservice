package com.example.auth_service.dto.events;

import java.time.LocalDateTime;

public record UserRegisteredEvent(
        Long userId,
        String firstName,
        String email,
        LocalDateTime createdAt
) {
}
