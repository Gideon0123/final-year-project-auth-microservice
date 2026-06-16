package com.example.auth_service.service;

import com.example.auth_service.dto.*;
import com.example.auth_service.entity.EmailVerificationToken;
import com.example.auth_service.entity.User;
import com.example.auth_service.enums.AccountStatus;
import com.example.auth_service.enums.Role;
import com.example.auth_service.exception.*;
import com.example.auth_service.mapper.UserMapper;
import com.example.auth_service.mapper.UserResponseMapper;
import com.example.auth_service.payload.PagedResponse;
import com.example.auth_service.repository.EmailVerificationTokenRepository;
import com.example.auth_service.repository.RefreshTokenRepository;
import com.example.auth_service.repository.UserRepository;
import com.example.auth_service.repository.specification.UserSpecificationBuilder;
import com.example.auth_service.util.CacheKeys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
    private final HttpServletRequest httpRequest;
    private final JwtService jwtService;

    @Transactional
    @CacheEvict(value = CacheKeys.USER, allEntries = true)
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
    @Cacheable(value = CacheKeys.USER, key = "#id")
    public UserProfileResponse getUserById(Long userId) {

        User user = userRepository.findByIdAndStatusNot(
                    userId,
                    AccountStatus.DELETED
                )
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        System.out.println("Data Coming From the DataBase 1");

        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheKeys.USER, key = "#page + '-' + #size + '-' + #sortBy")
    public PagedResponse<UserResponseDTO> getAllUsers(
            int page, int size, String sortBy
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));

        Page<UserResponseDTO> dtoPage = userRepository.findAllByStatusNot(AccountStatus.DELETED, pageable)
                .map(mapper::toResponse);

        System.out.println("Data Coming From the DataBase 2");

        return new PagedResponse<>(dtoPage);
    }

    @Transactional
    @CacheEvict(value = CacheKeys.USER, allEntries = true)
    public UpdateUserResponse updateUser(
            Long targetUserId,
            UpdateUserRequest request
    ) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

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

            emailVerificationTokenRepository.deleteByUserId(
                    target.getId()
            );

            target.setEmail(request.email());
            target.setEmailVerified(false);

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

            String accessToken = jwtService.extractToken(httpRequest);
            jwtService.blacklistToken(accessToken);

            auditService.log(
                    target.getId(),
                    "EMAIL_CHANGED",
                    "Email changed from "
                            + oldEmail
                            + " to "
                            + request.email(),
                    httpRequest.getRemoteAddr()
            );

        }

        userRepository.save(target);
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
    @CacheEvict(value = CacheKeys.USER, allEntries = true)
    public void disableUser(Long id) {

        User user = getUserEntity(id);

        user.setStatus(AccountStatus.DISABLED);

        userRepository.save(user);
    }

    @Transactional
    @CacheEvict(value = CacheKeys.USER, allEntries = true)
    public void enableUser(Long id) {

        User user = getUserEntity(id);

        user.setStatus(AccountStatus.ACTIVE);

        userRepository.save(user);
    }

    @Transactional
    @CacheEvict(value = CacheKeys.USER, allEntries = true)
    public void lockUser(Long id) {

        User user = getUserEntity(id);

        user.setStatus(AccountStatus.LOCKED);

        userRepository.save(user);
    }

    @Transactional
    @CacheEvict(value = CacheKeys.USER, allEntries = true)
    public void unlockUser(Long id) {

        User user = getUserEntity(id);

        user.setStatus(AccountStatus.ACTIVE);

        user.setFailedLoginAttempts(0);

        userRepository.save(user);
    }

    @Transactional
    @CacheEvict(value = CacheKeys.USER, allEntries = true)
    public SuspendUserResponse suspendUser(
            Long id,
            int days
    ) {

        User user = getUserEntity(id);

        user.setStatus(AccountStatus.SUSPENDED);

        user.setSuspendedUntil(LocalDateTime.now().plusDays(days));

        userMapper.toResponse(user);

        userRepository.save(user);

        return SuspendUserResponse.builder()
                .user(mapper.toResponse(user))
                .suspended(true)
                .suspendedUntil(user.getSuspendedUntil())
                .build();
    }

    @Transactional
    @CacheEvict(value = CacheKeys.USER, allEntries = true)
    public void deleteUser(Long id) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) {
            throw new AccessDeniedException("Not authenticated");
        }

        User currentUser = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new UserNotFoundException("You are not logged in"));

        boolean admin = currentUser.getRole() == Role.ADMIN;
        boolean owner = currentUser.getId().equals(id);

        if (!admin && !owner) {
            throw new AccessDeniedException("You cannot update this user");
        }

        User user = getUserEntity(id);

        user.setStatus(AccountStatus.DELETED);

        user.setDeletedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    @Transactional
    public Page<UserProfileResponse> searchUsers(

            String keyword,
            Long id,

            String firstName,
            String lastName,
            String username,
            String email,
            String phoneNo,

            Role role,
            AccountStatus status,

            Boolean emailVerified,
            Boolean accountNonLocked,

            LocalDateTime createdAfter,
            LocalDateTime createdBefore,

            Pageable pageable
    ) {

        Specification<User> spec =
                UserSpecificationBuilder.build(

                        keyword,
                        id,

                        firstName,
                        lastName,
                        username,
                        email,
                        phoneNo,

                        role,
                        status,

                        emailVerified,
                        accountNonLocked,

                        createdAfter,
                        createdBefore
                );

        Page<User> page = userRepository.findAll(spec, pageable);

        return page.map(userMapper::toResponse);
    }

    private User getUserEntity(Long id) {

        return userRepository.findByIdAndStatusNot(id, AccountStatus.DELETED)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
}