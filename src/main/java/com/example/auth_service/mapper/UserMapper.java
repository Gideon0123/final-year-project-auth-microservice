package com.example.auth_service.mapper;

import com.example.auth_service.dto.UserProfileResponse;
import com.example.auth_service.entity.User;
import lombok.Builder;
import org.springframework.stereotype.Component;

@Component
@Builder
public class UserMapper {

    public UserProfileResponse toResponse(User user) {

        return UserProfileResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .username(user.getUsername())
                .phoneNo(user.getPhoneNo())
                .role(user.getRole().name())
                .department(user.getDepartment())
                .faculty(user.getFaculty())
                .institution(user.getInstitution())
                .status(user.getStatus())
                .accountNonLocked(user.isAccountNonLocked())
                .emailVerified(user.isEmailVerified())
                .registeredAt(user.getRegisteredAt())
                .build();
    }
}