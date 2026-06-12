package com.example.auth_service.dto;

import com.example.auth_service.enums.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(
        @NotNull
        Role role
) {
}
