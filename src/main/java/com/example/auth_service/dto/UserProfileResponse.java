package com.example.auth_service.dto;

import com.example.auth_service.enums.AccountStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class UserProfileResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String username;
    private String phoneNo;
    private String role;
    private String department;
    private String faculty;
    private String institution;
    private AccountStatus status;
    private boolean emailVerified;
    private boolean accountNonLocked;
    private LocalDateTime registeredAt;
}
