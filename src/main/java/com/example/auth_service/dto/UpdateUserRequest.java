package com.example.auth_service.dto;

import jakarta.validation.constraints.Email;

public record UpdateUserRequest(

        String firstName,
        String lastName,
        String username,

        @Email
        String email,
        String phoneNo

) {}