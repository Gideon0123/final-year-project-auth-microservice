package com.example.auth_service.dto.events;

public record AccountDeletedEvent(
        Long userId,
        String email,
        String firstName
) {
}