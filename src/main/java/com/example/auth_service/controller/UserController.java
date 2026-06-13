package com.example.auth_service.controller;

import com.example.auth_service.dto.UpdateUserRequest;
import com.example.auth_service.dto.UserProfileResponse;
import com.example.auth_service.dto.UserResponseDTO;
import com.example.auth_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    @PreAuthorize("isAuthenticated()")
    public UserProfileResponse updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest request
    ) {
        return userService.updateUser(
                id,
                request
        );
    }
}