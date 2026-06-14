package com.example.auth_service.service;

import com.example.auth_service.dto.UpdateUserRequest;
import com.example.auth_service.dto.UpdateUserResponse;
import com.example.auth_service.dto.UserProfileResponse;
import com.example.auth_service.dto.UserResponseDTO;
import com.example.auth_service.entity.EmailVerificationToken;
import com.example.auth_service.entity.User;
import com.example.auth_service.enums.AccountStatus;
import com.example.auth_service.enums.Role;
import com.example.auth_service.exception.*;
import com.example.auth_service.mapper.UserMapper;
import com.example.auth_service.mapper.UserResponseMapper;
import com.example.auth_service.repository.EmailVerificationTokenRepository;
import com.example.auth_service.repository.RefreshTokenRepository;
import com.example.auth_service.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserResponseMapper mapper;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditService auditService;
    private final HttpServletRequest request;

    @Transactional
    public UserResponseDTO updateRole(
            Long userId,
            Role role,
            @AuthenticationPrincipal Authentication authentication
    ) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        boolean isAdmin = user.getRole().name().equalsIgnoreCase("ADMIN");
        boolean isOwner = user.getId().equals(existingUser.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You are not allowed to Update this account");
        }

        user.setRole(role);

        userRepository.save(user);
        return null;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserById(Long userId) {

        User user = userRepository.findByIdAndStatusNot(
                    userId,
                    AccountStatus.DELETED
                )
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponseDTO> getAllUsers(
            Pageable pageable
    ) {
        return userRepository.findAllByStatusNot(AccountStatus.DELETED, pageable)
                .map(mapper::toResponse);
    }

    @Transactional
    public UpdateUserResponse updateUser(
            Long targetUserId,
            UpdateUserRequest request
    ) {

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) {
            throw new AccessDeniedException("Not authenticated");
        }

        User currentUser = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new UserNotFoundException("You are not logged in"));

        boolean admin = currentUser.getRole() == Role.ADMIN;

        boolean owner = currentUser.getId().equals(targetUserId);

        if (!admin && !owner) {
            throw new AccessDeniedException("You cannot update this user");
        }

        User target = userRepository.findByIdAndStatusNot(
                        targetUserId,
                        AccountStatus.DELETED
                )
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (request.firstName() != null) {
            target.setFirstName(request.firstName());
        }

        if (request.lastName() != null) {
            target.setLastName(request.lastName());
        }

        if (request.username() != null &&
                !request.username().equalsIgnoreCase(target.getUsername())) {

            if (userRepository.existsByUsernameAndIdNot(
                    request.username(),
                    target.getId())) {

                throw new UsernameAlreadyExistsException("Username already exists");
            }

            target.setUsername(request.username());
        }

        if (request.phoneNo() != null
                && !request.phoneNo().equals(target.getPhoneNo())) {

            if (userRepository.existsByPhoneNoAndIdNot(
                    request.phoneNo(),
                    target.getId())) {

                throw new PhoneNumberAlreadyExistsException("Phone number already exists");
            }

            target.setPhoneNo(request.phoneNo());
        }

        String token = null;
        if (request.email() != null &&
                !request.email().equalsIgnoreCase(target.getEmail())) {

            String oldEmail = target.getEmail();

            if (userRepository.existsByEmailAndIdNot(
                    request.email(),
                    target.getId())) {

                throw new EmailAlreadyExistsException("Email already exists");
            }

            target.setEmail(request.email());
            target.setEmailVerified(false);

            emailVerificationTokenRepository.deleteByUserId(
                    target.getId()
            );

            token = UUID.randomUUID().toString();

            EmailVerificationToken verificationToken =
                    EmailVerificationToken.builder()
                            .token(token)
                            .user(target)
                            .expiryDate(LocalDateTime.now().plusDays(14))
                            .build();

            emailVerificationTokenRepository.save(
                    verificationToken
            );

            refreshTokenRepository.deleteByUserId(
                    target.getId()
            );

            auditService.log(
                    target.getId(),
                    "EMAIL_CHANGED",
                    "Email changed from "
                            + oldEmail
                            + " to "
                            + request.email()
//                    request.getRemoteAddr()
            );

            userRepository.save(target);

        }
        /*
        Later replace with RabbitMQ event

        emailProducer.sendVerificationEmail(
                target.getEmail(),
                verificationTokenValue
        );
        */
        return UpdateUserResponse.builder()
                .user(userMapper.toResponse(target))
                .verificationToken(token)
                .build();
    }

    @Transactional
    public void disableUser(Long id) {

        User user = getUserEntity(id);

        user.setStatus(AccountStatus.DISABLED);

        userRepository.save(user);
    }

    @Transactional
    public void enableUser(Long id) {

        User user = getUserEntity(id);

        user.setStatus(AccountStatus.ACTIVE);

        userRepository.save(user);
    }

    @Transactional
    public void lockUser(Long id) {

        User user = getUserEntity(id);

        user.setStatus(AccountStatus.LOCKED);

        userRepository.save(user);
    }

    @Transactional
    public void unlockUser(Long id) {

        User user = getUserEntity(id);

        user.setStatus(AccountStatus.ACTIVE);

        user.setFailedLoginAttempts(0);

        userRepository.save(user);
    }

    @Transactional
    public void suspendUser(
            Long id,
            int days
    ) {

        User user = getUserEntity(id);

        user.setStatus(AccountStatus.SUSPENDED);

        user.setSuspendedUntil(LocalDateTime.now().plusDays(days));

        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {

        User user = getUserEntity(id);

        user.setStatus(AccountStatus.DELETED);

        user.setDeletedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    private User getUserEntity(Long id) {

        return userRepository.findByIdAndStatusNot(id, AccountStatus.DELETED)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
}