package com.example.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuspendUserResponse {

    private UserResponseDTO  user;
    private Boolean suspended;
    private LocalDateTime suspendedUntil;
}
