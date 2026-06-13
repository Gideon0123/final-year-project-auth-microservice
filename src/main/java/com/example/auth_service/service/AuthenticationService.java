package com.example.auth_service.service;

import com.example.auth_service.dto.*;
import com.example.auth_service.entity.*;
import com.example.auth_service.enums.AccountStatus;
import com.example.auth_service.enums.Role;
import com.example.auth_service.exception.*;
import com.example.auth_service.mapper.UserMapper;
import com.example.auth_service.mapper.UserResponseMapper;
import com.example.auth_service.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
    private final LoginAuditRepository loginAuditRepository;
    private final UserResponseMapper mapper;
    private final UserMapper userMapper;

    public String register(
            RegisterRequest request
    ) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException("Username already exists");
        }

        if (userRepository.existsByPhoneNo(request.getPhoneNo())) {
            throw new PhoneNumberAlreadyExistsException("Phone number already exists");
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
                .role(Role.STUDENT)
                .registeredAt(LocalDateTime.now())
                .status(AccountStatus.LOCKED)
                .department(request.getDepartment())
                .faculty(request.getFaculty())
                .institution(request.getInstitution())
                .failedLoginAttempts(0)
                .enabled(true)
                .accountNonLocked(true)
                .emailVerified(false)
                .build();

        userRepository.save(user);

        String token = UUID.randomUUID().toString();

        EmailVerificationToken verificationToken =
                EmailVerificationToken.builder()
                        .token(token)
                        .user(user)
                        .expiryDate(LocalDateTime.now().plusDays(14))
                        .build();

        emailVerificationTokenRepository.save(verificationToken);
//        rabbitTemplate.convertAndSend(
//                "notification.exchange",
//                "email.verification",
//
//                new EmailVerificationEvent(user.getEmail(), token)
//        );
        return token;
    }

    public LoginResponseDTO login(
            LoginRequest request
    ) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid Credentials"));

        if (user.getStatus() == AccountStatus.DISABLED || user.getStatus() == AccountStatus.SUSPENDED) {
            throw new AccountDisabledException("Account " + user.getStatus());
        }

        verifyAccountLock(user);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (org.springframework.security.authentication.DisabledException ex) {
            throw new DisabledException("Please verify your email before logging in");

        } catch (LockedException ex) {
            throw new AccountLockedException("Account is locked");

        } catch (BadCredentialsException ex) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= MAX_ATTEMPTS) {
                lockUser(user);
            }

            loginAuditRepository.save(
                    LoginAudit.builder()
                            .email(request.getEmail())
                            .successful(false)
                            .loginTime(LocalDateTime.now())
                            .build()
            );
            userRepository.save(user);
            throw new InvalidCredentialsException("Invalid credentials");
        }

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Verify your email first");
        }

        switch (user.getStatus()) {

            case ACTIVE -> {}

            case LOCKED -> throw new AccountLockedException("Account locked");

            case DISABLED -> throw new AccountDisabledException("Account disabled");

            case SUSPENDED -> throw new AccountSuspendedException(
                    "Account suspended until " + user.getSuspendedUntil()
            );

            case DELETED -> throw new AccountDeletedException("Account deleted");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

//        refreshTokenRepository.findByUser(user)
//                .ifPresent(refreshTokenRepository::delete);

        RefreshToken entity = RefreshToken.builder()
                .token(refreshToken)
                .user(user)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();

        loginAuditRepository.save(
                LoginAudit.builder()
                        .email(user.getEmail())
                        .successful(true)
                        .loginTime(LocalDateTime.now())
                        .build()
        );

        refreshTokenRepository.save(entity);

        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

        UserResponseDTO userResponse = mapper.toResponse(user);
//        UserResponseDTO.builder()
//                .userId(user.getId())
//                .firstName(user.getFirstName())
//                .lastName(user.getLastName())
//                .email(user.getEmail())
//                .phoneNo(user.getPhoneNo())
//                .role(user.getRole().name())
//                .status(user.getStatus())
//                .build();

        return LoginResponseDTO.builder()
                .authResponse(authResponse)
                .userResponse(userResponse)
                .build();
    }

    public LoginResponseDTO refreshToken(
            RefreshTokenRequest request
    ) {
        if (!jwtService.validateRefreshToken(request.getRefreshToken())) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        RefreshToken tokenEntity = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (tokenEntity.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Refresh token expired");
        }

        User user = tokenEntity.getUser();

        String newAccessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .build();

        UserResponseDTO userResponse = mapper.toResponse(user);

        return LoginResponseDTO.builder()
                .authResponse(authResponse)
                .userResponse(userResponse)
                .build();
    }

    public void logout(
            String accessToken
    ) {
        String email = jwtService.extractUsername(accessToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        jwtService.blacklistToken(accessToken);

        refreshTokenRepository.deleteByUser(user);
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
        user.setEnabled(true);
        user.setAccountNonLocked(true);
        user.setStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        verificationToken.setUsed(true);
        emailVerificationTokenRepository.save(verificationToken);
    }

    public String forgotPassword(
            ForgotPasswordRequest request
    ) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(()  -> new UserNotFoundException("User not found"));

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(1))
                .build();

        passwordResetTokenRepository.deleteByUser(user);
        passwordResetTokenRepository.save(resetToken);
//        rabbitTemplate.convertAndSend(
//                "notification.exchange",
//                "password.reset",
//
//                new PasswordResetEvent(email, token)
//        );
        return token;
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
        refreshTokenRepository.deleteAllByUser(user);
    }

    private void lockUser(
            User user
    ) {
        user.setAccountNonLocked(false);
        user.setStatus(AccountStatus.LOCKED);
        user.setLockTime(LocalDateTime.now());
        userRepository.save(user);
    }

    private void unlockUser(
            User user
    ) {
        user.setAccountNonLocked(true);
        user.setStatus(AccountStatus.ACTIVE);
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

        if (user.getLockTime().plusMinutes(LOCK_DURATION_MINUTES).isBefore(LocalDateTime.now())) {
            unlockUser(user);
            user.setStatus(AccountStatus.ACTIVE);
            return;
        }
        throw new AccountLockedException("Account is temporarily locked");
    }

    public void changePassword(
            String email,
            ChangePasswordRequest request
    ) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Incorrect old password");
        }

        user.setPassword(
                passwordEncoder.encode(request.getNewPassword())
        );

        userRepository.save(user);
        refreshTokenRepository.deleteAllByUser(user);
    }

    public UserProfileResponse getCurrentUser(
            Authentication authentication
    ) {
        if (authentication == null) {
            throw new InvalidCredentialsException("User Not Logged in");
        }

        String email = authentication.getName();

        if (email == null) {
            throw new InvalidCredentialsException("User not Authenticated");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return userMapper.toResponse(user);
    }

    public String resendVerificationEmail(
            ResendVerificationRequest request
    ) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.isEmailVerified()) {
            throw new EmailAlreadyVerifiedException("Email already verified");
        }

        emailVerificationTokenRepository.findByUser(user)
                .ifPresent(emailVerificationTokenRepository::delete);

        emailVerificationTokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();

        EmailVerificationToken verificationToken =
                EmailVerificationToken.builder()
                        .token(token)
                        .user(user)
                        .expiryDate(LocalDateTime.now().plusHours(24))
                        .build();

        emailVerificationTokenRepository.save(verificationToken);
        // RabbitMQ event later
        return token;
    }

    public void logoutAllDevices(
            String email
    ) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        refreshTokenRepository.deleteAllByUser(user);
    }
}