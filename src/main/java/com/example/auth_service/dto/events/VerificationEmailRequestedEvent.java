package com.example.auth_service.dto.events;

import java.time.LocalDateTime;

public record VerificationEmailRequestedEvent(

        Long userId,
        String email,
        String firstName,
        String verificationToken,
        LocalDateTime requestedAt

) {}