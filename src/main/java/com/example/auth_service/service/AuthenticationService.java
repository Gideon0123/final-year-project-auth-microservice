package com.example.auth_service.service;

import com.example.auth_service.dto.*;
import com.example.auth_service.entity.EmailVerificationToken;
import com.example.auth_service.entity.PasswordResetToken;
import com.example.auth_service.entity.RefreshToken;
import com.example.auth_service.entity.User;
import com.example.auth_service.exception.*;
import com.example.auth_service.repository.EmailVerificationTokenRepository;
import com.example.auth_service.repository.PasswordResetTokenRepository;
import com.example.auth_service.repository.RefreshTokenRepository;
import com.example.auth_service.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RabbitTemplate rabbitTemplate;
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    public void register(
            RegisterRequest request
    ) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException("Username already exists");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .username(request.getUsername())
                .phoneNo(request.getPhoneNo())
                .password(
                        passwordEncoder.encode(
                                request.getPassword()
                        )
                )
                .department(request.getDepartment())
                .faculty(request.getFaculty())
                .institution(request.getInstitution())
                .build();

        userRepository.save(user);

        String token = UUID.randomUUID().toString();

        EmailVerificationToken verificationToken =
                EmailVerificationToken.builder()
                        .token(token)
                        .user(user)
                        .expiryDate(LocalDateTime.now().plusHours(24))
                        .build();

        emailVerificationTokenRepository.save(verificationToken);

        rabbitTemplate.convertAndSend(
                "notification.exchange",
                "email.verification",

                new EmailVerificationEvent(user.getEmail(), token)
        );
    }

    public AuthResponse login(
            LoginRequest request
    ) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Verify your email first");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        }
        catch (BadCredentialsException ex) {

            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= MAX_ATTEMPTS) {
                lockUser(user);
            }
            userRepository.save(user);
            throw new InvalidCredentialsException("Invalid credentials");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        refreshTokenRepository.findByUser(user)
                .ifPresent(refreshTokenRepository::delete);

        RefreshToken entity = RefreshToken.builder()
                .token(refreshToken)
                .user(user)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();

        ResponseCookie accessCookie = jwtService.buildAccessCookie(accessToken);
        ResponseCookie refreshCookie = jwtService.buildRefreshCookie(refreshToken);

        refreshTokenRepository.save(entity);

        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    public AuthResponse refreshToken(
            RefreshTokenRequest request
    ) {
        if (!jwtService.validateRefreshToken(request.getRefreshToken())) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        RefreshToken tokenEntity = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        User user = tokenEntity.getUser();

        String newAccessToken = jwtService.generateAccessToken(user);

        return AuthResponse.builder()

                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken())
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    public void logout(
            String accessToken,
            String refreshToken
    ) {
        jwtService.blacklistToken(accessToken);

        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(refreshTokenRepository::delete);
    }

    public void verifyEmail(
            String token
    ) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository
                .findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid token"));

        if (verificationToken.isUsed()) {
            throw new InvalidTokenException("Token already used");
        }

        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Token expired");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        emailVerificationTokenRepository.save(verificationToken);
    }

    public void forgotPassword(
            String email
    ) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(()  -> new UserNotFoundException("User not found"));

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(1))
                .build();

        passwordResetTokenRepository.save(resetToken);

        rabbitTemplate.convertAndSend(
                "notification.exchange",
                "password.reset",

                new PasswordResetEvent(email, token)
        );
    }

    public void resetPassword(
            ResetPasswordRequest request
    ) {

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(()  -> new InvalidTokenException("Invalid token"));

        if (resetToken.isUsed()) {
            throw new InvalidTokenException("Token already used");
        }

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Token expired");
        }

        User user = resetToken.getUser();

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    private void lockUser(
            User user
    ) {
        user.setAccountNonLocked(false);
        user.setLockTime(LocalDateTime.now());
        userRepository.save(user);
    }

    private void unlockUser(
            User user
    ) {
        user.setAccountNonLocked(true);
        user.setFailedLoginAttempts(0);
        user.setLockTime(null);
        userRepository.save(user);
    }

    private void verifyAccountLock(
            User user
    ) {

        if (user.isAccountNonLocked()) {
            return;
        }

        if (user.getLockTime().plusMinutes(LOCK_DURATION_MINUTES).isBefore(LocalDateTime.now())
        ) {
            unlockUser(user);
            return;
        }
        throw new AccountLockedException("Account is temporarily locked");
    }

    public void changePassword(
            String email,
            ChangePasswordRequest request
    ) {
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Incorrect old password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    }

    public UserProfileResponse getCurrentUser(
            String email
    ) {

    }

    public void resendVerificationEmail(String email)
}
