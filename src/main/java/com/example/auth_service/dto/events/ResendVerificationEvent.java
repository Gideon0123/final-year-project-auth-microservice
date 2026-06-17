package com.example.auth_service.dto.events;

public record ResendVerificationEvent(
        Long userId,
        String email,
        String firstName,
        String verificationToken
) {
}
