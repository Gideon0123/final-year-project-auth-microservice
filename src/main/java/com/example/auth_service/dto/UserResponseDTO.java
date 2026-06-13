package com.example.auth_service.dto;

import com.example.auth_service.enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {

    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String username;
    private String phoneNo;
    private String role;

    private AccountStatus status;
    private boolean accountNonLocked;

}
