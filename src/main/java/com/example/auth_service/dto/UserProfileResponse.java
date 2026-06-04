package com.example.auth_service.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UserProfileResponse {

    private Long id;

    private String firstName;

    private String lastName;

    private String email;

    private String username;

    private String role;

    private String department;

    private String faculty;

    private String institution;
}
