package com.example.auth_service.dto;

import lombok.Builder;

@Builder
public record UserSummaryResponse(

        Long id,
        String fullName,
        String email,
        String institution,
        String faculty,
        String department

) {}