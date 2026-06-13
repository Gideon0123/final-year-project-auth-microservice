package com.example.auth_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SuspendUserRequest(

        @Min(1)
        @Max(365)
        int days
) {
}