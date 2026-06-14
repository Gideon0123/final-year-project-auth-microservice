package com.example.auth_service.controller;

import com.example.auth_service.dto.*;
import com.example.auth_service.service.UserService;
import com.example.auth_service.util.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserProfileResponse getUser(
            @PathVariable Long id
    ) {
        return userService.getUserById(id);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserResponseDTO> getAllUsers(
            Pageable pageable
    ) {
        return userService.getAllUsers(pageable);
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
}