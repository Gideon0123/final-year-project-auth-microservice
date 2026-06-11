package com.example.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;

//    private Long userId;
//    private String email;
//    private String role;
}