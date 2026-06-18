package com.example.auth_service.dto.events;

import java.time.LocalDateTime;

public record PasswordResetRequestedEvent(

        Long userId,
        String email,
        String firstName,
        String token,
        LocalDateTime requestedAt

) {}