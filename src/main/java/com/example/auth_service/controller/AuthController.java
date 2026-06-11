package com.example.auth_service.controller;

import com.example.auth_service.dto.*;
import com.example.auth_service.security.UserPrincipal;
import com.example.auth_service.service.AuthenticationService;
import com.example.auth_service.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthenticationService authenticationService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Object>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        String token = authenticationService.register(request);

        return ResponseEntity.ok(
                ApiResponse.builder()
                        .success(true)
                        .status(201)
                        .data(token)
                        .message("Registration successful")
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {

        AuthResponse response = authenticationService.login(request);

        ResponseCookie accessCookie = jwtService.buildAccessCookie(
                response.getAccessToken()
        );

        ResponseCookie refreshCookie = jwtService.buildRefreshCookie(
                response.getRefreshToken()
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        accessCookie.toString()
                )
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie.toString()
                )
                .body(
                        ApiResponse.<AuthResponse>builder()
                                .success(true)
                                .status(200)
                                .message("Login successful")
                                .data(response)
                                .timestamp(LocalDateTime.now())
                                .build()
                );
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @CookieValue(value = "refreshToken", required = false)
            String refreshToken
    ) {
        RefreshTokenRequest request = new RefreshTokenRequest();

        request.setRefreshToken(refreshToken);

        AuthResponse response = authenticationService.refreshToken(request);

        return ResponseEntity.ok(
                ApiResponse.<AuthResponse>builder()
                        .success(true)
                        .status(200)
                        .message("Token refreshed")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(
            HttpServletRequest request
    ) {
        String token = jwtService.extractToken(request);

        authenticationService.logout(token);

        ResponseCookie clearAccess = ResponseCookie.from(
                        "accessToken",
                        ""
                )
                .maxAge(0)
                .path("/")
                .build();

        ResponseCookie clearRefresh = ResponseCookie.from(
                        "refreshToken",
                        ""
                )
                .maxAge(0)
                .path("/")
                .build();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        clearAccess.toString()
                )
                .header(
                        HttpHeaders.SET_COOKIE,
                        clearRefresh.toString()
                )
                .body(
                        ApiResponse.builder()
                                .success(true)
                                .status(200)
                                .message("Logged Out")
                                .timestamp(LocalDateTime.now())
                                .build()
                );
    }

    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Object>> logoutAllDevices(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        authenticationService.logoutAllDevices(
                userPrincipal.getEmail()
        );

        ResponseCookie clearAccess = ResponseCookie.from(
                        "accessToken",
                        ""
                )
                .maxAge(0)
                .path("/")
                .build();

        ResponseCookie clearRefresh = ResponseCookie.from(
                        "refreshToken",
                        ""
                )
                .maxAge(0)
                .path("/")
                .build();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        clearAccess.toString()
                )
                .header(
                        HttpHeaders.SET_COOKIE,
                        clearRefresh.toString()
                )
                .body(
                        ApiResponse.builder()
                                .success(true)
                                .message("All Devices Logged Out")
                                .timestamp(LocalDateTime.now())
                                .build()
                );
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Object>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authenticationService.changePassword(
                authentication.getName(),
                request
        );

        return ResponseEntity.ok(
                ApiResponse.builder()
                        .success(true)
                        .message(
                                "Password changed successfully"
                        )
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> me(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                ApiResponse.<UserProfileResponse>builder()
                        .success(true)
                        .message("User retrieved")
                        .data(
                                authenticationService
                                        .getCurrentUser(
                                                authentication.getName()
                                        )
                        )
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Object>> verifyEmail(
            @RequestParam String token
    ) {
        authenticationService.verifyEmail(token);

        return ResponseEntity.ok(
                ApiResponse.builder()
                        .success(true)
                        .status(200)
                        .message("Email verified successfully")
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Object>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request
    ) {
        authenticationService.resendVerificationEmail(request);

        return ResponseEntity.ok(
                ApiResponse.builder()
                        .success(true)
                        .message("Verification email sent")
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Object>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {

        authenticationService.forgotPassword(request);

        return ResponseEntity.ok(
                ApiResponse.builder()
                        .success(true)
                        .message("Password reset email sent")
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Object>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {

        authenticationService.resetPassword(request);

        return ResponseEntity.ok(
                ApiResponse.builder()
                        .success(true)
                        .message("Password reset successful")
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

}

//The next logical step is RabbitMQ integration, because Auth Service is the first service that will publish events consumed by the Notification Service. That lays the foundation for the rest of the microservices.

//@Bean
//public CommandLineRunner seedAdmin(
//        UserRepository userRepository,
//        PasswordEncoder passwordEncoder
//) {
//
//    return args -> {
//
//        if (!userRepository.existsByEmail("admin@unizik.edu.ng")) {
//
//            User admin = User.builder()
//                    .firstName("System")
//                    .lastName("Admin")
//                    .email("admin@unizik.edu.ng")
//                    .username("admin")
//                    .password(
//                            passwordEncoder.encode("Admin123!")
//                    )
//                    .role(Role.ADMIN)
//                    .emailVerified(true)
//                    .build();
//
//            userRepository.save(admin);
//        }
//    };
//}