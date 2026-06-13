package com.example.auth_service.mapper;

import com.example.auth_service.dto.UserResponseDTO;
import com.example.auth_service.entity.User;
import lombok.Builder;
import org.springframework.stereotype.Component;

@Component
@Builder
public class UserResponseMapper {

    public UserResponseDTO toResponse(User user) {
        return UserResponseDTO.builder()
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .username(user.getUsername())
                .phoneNo(user.getPhoneNo())
                .role(user.getRole().name())
                .status(user.getStatus())
                .accountNonLocked(user.isAccountNonLocked())
                .build();
    }
}
