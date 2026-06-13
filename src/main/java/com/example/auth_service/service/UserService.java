package com.example.auth_service.service;

import com.example.auth_service.dto.UpdateUserRequest;
import com.example.auth_service.dto.UserProfileResponse;
import com.example.auth_service.dto.UserResponseDTO;
import com.example.auth_service.entity.User;
import com.example.auth_service.enums.AccountStatus;
import com.example.auth_service.enums.Role;
import com.example.auth_service.exception.*;
import com.example.auth_service.mapper.UserMapper;
import com.example.auth_service.mapper.UserResponseMapper;
import com.example.auth_service.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserResponseMapper mapper;

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
    public UserProfileResponse updateUser(
            Long targetUserId,
            UpdateUserRequest request
    ) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        assert auth != null;
        User currentUser = userRepository.findByEmail(
                        auth.getName()
                )
                .orElseThrow(() -> new UserNotFoundException("You are not Logged in"));

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

        if (request.username() != null) {
            if (userRepository.existsByUsernameAndIdNot(request.username(), target.getId())) {
                throw new UsernameAlreadyExistsException("Username already exists");
            }
            target.setUsername(request.username());
        }

        if (request.email() != null) {
            if (userRepository.existsByEmailAndIdNot(request.email(), target.getId())) {
                throw new UsernameAlreadyExistsException("Email already exists");
            }
            target.setEmail(request.email());
            target.setEmailVerified(false);

            createVerificationToken(target);

            sendVerificationEmail(target);
        }

        if (request.phoneNo() != null) {
            if (userRepository.existsByPhoneNoAndIdNot(request.phoneNo(), target.getId())) {
                throw new UsernameAlreadyExistsException("Phone number already exists");
            }
            target.setPhoneNo(request.phoneNo());
        }

        userRepository.save(target);

        return userMapper.toResponse(target);
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
