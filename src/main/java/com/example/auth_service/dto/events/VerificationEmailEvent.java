package com.example.auth_service.dto.events;

public record VerificationEmailEvent(
        Long userId,
        String email,
        String firstName,
        String verificationToken
) {
}