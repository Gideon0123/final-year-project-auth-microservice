package com.example.auth_service.controller;

import com.example.auth_service.dto.*;
import com.example.auth_service.payload.PagedResponse;
import com.example.auth_service.service.UserService;
import com.example.auth_service.util.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUser(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        UserProfileResponse response = userService.getUserById(id);

        return ResponseEntity.ok(
                ApiResponse.<UserProfileResponse>builder()
                        .success(true)
                        .message("User Fetched successfully")
                        .status(200)
                        .data(response)
                        .errors(null)
                        .path(request.getRequestURI())
                        .traceId(TraceIdUtil.generate())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponseDTO>>> getAllUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            HttpServletRequest request
    ) {
        int adjustedPage = Math.max(page - 1, 0);
        PagedResponse<UserResponseDTO> users = userService.getAllUsers(adjustedPage, size, sortBy);
        PagedResponse<UserResponseDTO> response = PagedResponse.<UserResponseDTO>builder()
                .content(users.getContent())
                .size(users.getSize())
                .page(users.getPage())
                .first(users.isFirst())
                .last(users.isLast())
                .totalElements(users.getTotalElements())
                .totalPages(users.getTotalPages())
                .build();

        return ResponseEntity.ok(
                ApiResponse.<PagedResponse<UserResponseDTO>>builder()
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

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<UpdateUserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            HttpServletRequest httpRequest
    ) {
        UpdateUserResponse response = userService.updateUser(id, request);

        return ResponseEntity.ok(
                ApiResponse.<UpdateUserResponse>builder()
                        .success(true)
                        .status(200)
                        .message("User updated successfully")
                        .data(response)
                        .errors(null)
                        .path(httpRequest.getRequestURI())
                        .traceId(TraceIdUtil.generate())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @GetMapping("/internal/{id}")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> getUserSummary (
            @PathVariable Long id
    ) {
        UserSummaryResponse response = userService.getUserSummary(id);

        return ResponseEntity.ok(
                ApiResponse.<UserSummaryResponse>builder()
                        .success(true)
                        .message("User Summary successfully Fetched.")
                        .status(HttpStatus.OK.value())
                        .data(response)
                        .path(null)
                        .traceId(null)
                        .timestamp(null)
                        .build()
        );
    }

    @GetMapping("/internal/reviewers/{id}")
    public ResponseEntity<ApiResponse<ReviewerSummaryResponse>> getReviewerSummary(
            @PathVariable Long id
    ) {
        ReviewerSummaryResponse response = userService.getSummary(id);

        return ResponseEntity.ok(
                ApiResponse.<ReviewerSummaryResponse>builder()
                        .success(true)
                        .message("User Summary successfully Fetched.")
                        .status(HttpStatus.OK.value())
                        .data(response)
                        .path(null)
                        .traceId(null)
                        .timestamp(null)
                        .build()
        );
    }
}