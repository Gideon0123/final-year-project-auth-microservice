package com.example.auth_service.controller;

import com.example.auth_service.dto.*;
import com.example.auth_service.enums.AccountStatus;
import com.example.auth_service.enums.Role;
import com.example.auth_service.payload.PagedResponse;
import com.example.auth_service.service.UserService;
import com.example.auth_service.util.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("auth/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserService userService;

    @PatchMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> enableUser(
            @PathVariable Long id
    ) {
        userService.enableUser(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> disableUser(
            @PathVariable Long id
    ) {
        userService.disableUser(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> lockUser(
            @PathVariable Long id
    ) {
        userService.lockUser(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> unlockUser(
            @PathVariable Long id
    ) {
        userService.unlockUser(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(
            @PathVariable Long id
    ) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SuspendUserResponse>> suspendUser(
            @PathVariable Long id,
            @RequestBody @Valid SuspendUserRequest request,
            HttpServletRequest httpRequest
    ) {

        SuspendUserResponse response = userService.suspendUser(
                id,
                request.days()
        );

        return ResponseEntity.ok(
                ApiResponse.<SuspendUserResponse>builder()
                        .success(true)
                        .status(200)
                        .errors(null)
                        .data(response)
                        .message("User suspended successfully")
                        .path(httpRequest.getRequestURI())
                        .traceId(TraceIdUtil.generate())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedResponse<UserProfileResponse>>> searchUsers(

            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phoneNo,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) Boolean emailVerified,
            @RequestParam(required = false) Boolean accountNonLocked,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime createdBefore,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,

            HttpServletRequest request
    ) {

        int adjustedPage = Math.max(page - 1, 0);
        Pageable pageable = PageRequest.of(
                adjustedPage,
                size,
                Sort.by(sortBy)
        );

        Page<UserProfileResponse> usersPage = userService.searchUsers(

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
                createdBefore,
                pageable
        );

        PagedResponse<UserProfileResponse> response =
                PagedResponse.<UserProfileResponse>builder()
                        .content(usersPage.getContent())
                        .page(usersPage.getNumber() + 1)
                        .size(usersPage.getSize())
                        .totalElements(usersPage.getTotalElements())
                        .totalPages(usersPage.getTotalPages())
                        .first(usersPage.isFirst())
                        .last(usersPage.isLast())
                        .build();

        return ResponseEntity.ok(

                ApiResponse.<PagedResponse<UserProfileResponse>>builder()
                        .success(true)
                        .message("Users fetched successfully")
                        .status(200)
                        .data(response)
                        .errors(null)
                        .path(request.getRequestURI())
                        .traceId(TraceIdUtil.generate())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}