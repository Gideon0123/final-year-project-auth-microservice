package com.example.auth_service.dto;

import lombok.Builder;

@Builder
public record UserSearchResponse(

        Long id,
        String firstName,
        String lastName,
        String username,
        String email,
        String institution,
        String faculty,
        String department,
        String role

) {
}