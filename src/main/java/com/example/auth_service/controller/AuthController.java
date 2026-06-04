package com.example.auth_service.controller;

import com.example.auth_service.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;

//    POST /auth/register
//
//    POST /auth/login
//
//    POST /auth/refresh-token
//
//    POST /auth/logout
//
//    GET /auth/verify-email
//
//    POST /auth/resend-verification
//
//    POST /auth/forgot-password
//
//    POST /auth/reset-password
//
//    POST /auth/change-password
//
//    GET /auth/me
//
//    POST /auth/logout-all
}
