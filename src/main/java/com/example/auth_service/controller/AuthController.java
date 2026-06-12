package com.example.auth_service.controller;

import com.example.auth_service.dto.*;
import com.example.auth_service.security.UserPrincipal;
import com.example.auth_service.service.AuthenticationService;
import com.example.auth_service.service.JwtService;
import com.example.auth_service.util.CookieUtil;
import com.example.auth_service.util.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        String token = authenticationService.register(request);

        return ResponseEntity.ok(
                ApiResponse.builder()
                        .success(true)
                        .status(201)
                        .data(token)
                        .errors(null)
                        .message("Registration successful")
                        .path(httpRequest.getServletPath())
                        .traceId(TraceIdUtil.generate())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {

        LoginResponseDTO loginResponse = authenticationService.login(request);

//        ResponseCookie accessCookie = jwtService.buildAccessCookie(
//                loginResponse.getAuthResponse().getAccessToken()
//        );
        jwtService.buildAccessCookie(loginResponse.getAuthResponse().getAccessToken());
        jwtService.buildRefreshCookie(loginResponse.getAuthResponse().getRefreshToken());
//        ResponseCookie refreshCookie = jwtService.buildRefreshCookie(
//                loginResponse.getAuthResponse().getRefreshToken()
//        );

        CookieUtil.addAccessToken(response, loginResponse.getAuthResponse().getAccessToken());
        CookieUtil.addRefreshToken(response, loginResponse.getAuthResponse().getRefreshToken());

        ApiResponse<LoginResponseDTO> apiResponse =
                ApiResponse.<LoginResponseDTO>builder()
                        .success(true)
                        .message("Login successful")
                        .status(200)
                        .data(loginResponse)
                        .errors(null)
                        .path(httpRequest.getRequestURI())
                        .traceId(TraceIdUtil.generate())
                        .timestamp(LocalDateTime.now())
                        .build();

        return ResponseEntity.ok()
//                .header(
//                        HttpHeaders.SET_COOKIE,
//                        accessCookie.toString()
//                )
//                .header(
//                        HttpHeaders.SET_COOKIE,
//                        refreshCookie.toString()
//                )
                .body(apiResponse);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> refreshToken(
            @CookieValue(value = "refreshToken", required = false)
            String refreshToken,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        RefreshTokenRequest request = new RefreshTokenRequest();

        request.setRefreshToken(refreshToken);

//        AuthResponse response = authenticationService.refreshToken(request);
        LoginResponseDTO loginResponse = authenticationService.refreshToken(request);
        jwtService.buildAccessCookie(loginResponse.getAuthResponse().getAccessToken());
        jwtService.buildRefreshCookie(loginResponse.getAuthResponse().getRefreshToken());

        // SAVE NEW TOKENS
        CookieUtil.addAccessToken(httpResponse, loginResponse.getAuthResponse().getAccessToken());
        CookieUtil.addRefreshToken(httpResponse, loginResponse.getAuthResponse().getRefreshToken());

        ApiResponse<LoginResponseDTO> apiResponse =
                ApiResponse.<LoginResponseDTO>builder()
                        .success(true)
                        .message("Refreshed successfully")
                        .status(200)
                        .data(loginResponse)
                        .errors(null)
                        .path(httpRequest.getRequestURI())
                        .traceId(TraceIdUtil.generate())
                        .timestamp(LocalDateTime.now())
                        .build();

        return ResponseEntity.ok(
                apiResponse
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(
            HttpServletRequest request,
            HttpServletResponse response
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

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String authToken = authHeader.substring(7);
            jwtService.blacklistToken(authToken);
        }

        // CLEAR COOKIES
        CookieUtil.clearCookies(response);

        ApiResponse<Object> apiResponse =
                ApiResponse.builder()
                        .success(true)
                        .message("Logged out successfully")
                        .status(200)
                        .data(null)
                        .errors(null)
                        .path(request.getRequestURI())
                        .traceId(TraceIdUtil.generate())
                        .timestamp(LocalDateTime.now())
                        .build();

        return ResponseEntity.ok()
//                .header(
//                        HttpHeaders.SET_COOKIE,
//                        clearAccess.toString()
//                )
//                .header(
//                        HttpHeaders.SET_COOKIE,
//                        clearRefresh.toString()
//                )
                .body(apiResponse);
    }

    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Object>> logoutAllDevices(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            HttpServletRequest request,
            HttpServletResponse response
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

        CookieUtil.clearCookies(response);

        ApiResponse<Object> apiResponse =
                ApiResponse.builder()
                        .success(true)
                        .message("Logged out successfully")
                        .status(200)
                        .data(null)
                        .errors(null)
                        .path(request.getRequestURI())
                        .traceId(TraceIdUtil.generate())
                        .timestamp(LocalDateTime.now())
                        .build();

        return ResponseEntity.ok()
//                .header(
//                        HttpHeaders.SET_COOKIE,
//                        clearAccess.toString()
//                )
//                .header(
//                        HttpHeaders.SET_COOKIE,
//                        clearRefresh.toString()
//                )
                .body(apiResponse);
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Object>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        authenticationService.changePassword(
                authentication.getName(),
                request
        );

        return ResponseEntity.ok(
                ApiResponse.builder()
                        .success(true)
                        .message("Password Changed successfully")
                        .status(200)
                        .data(null)
                        .errors(null)
                        .path(httpRequest.getRequestURI())
                        .traceId(TraceIdUtil.generate())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> me(
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {

        UserProfileResponse response = authenticationService.getCurrentUser(authentication);

        return ResponseEntity.ok(
                ApiResponse.<UserProfileResponse>builder()
                        .success(true)
                        .message("User retrieved")
                        .status(200)
                        .errors(null)
                        .data(response)
                        .path(httpRequest.getRequestURI())
                        .traceId(TraceIdUtil.generate())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Object>> verifyEmail(
            @RequestParam String token,
            HttpServletRequest httpRequest
    ) {
        authenticationService.verifyEmail(token);

        return ResponseEntity.ok(
                ApiResponse.builder()
                        .success(true)
                        .status(200)
                        .message("Email verified successfully")
                        .errors(null)
                        .path(httpRequest.getRequestURI())
                        .traceId(TraceIdUtil.generate())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Object>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request,
            HttpServletRequest httpRequest
    ) {
        String token = authenticationService.resendVerificationEmail(request);

        return ResponseEntity.ok(
                ApiResponse.builder()
                        .success(true)
                        .status(200)
                        .data(token)
                        .errors(null)
                        .message("Verification email sent")
                        .path(httpRequest.getRequestURI())
                        .traceId(TraceIdUtil.generate())
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