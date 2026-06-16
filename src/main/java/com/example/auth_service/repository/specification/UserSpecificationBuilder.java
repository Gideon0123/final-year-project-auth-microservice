package com.example.auth_service.repository.specification;

import com.example.auth_service.entity.User;
import com.example.auth_service.enums.AccountStatus;
import com.example.auth_service.enums.Role;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserSpecificationBuilder {

    public static Specification<User> build(

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
            LocalDateTime createdBefore

    ) {

        return Specification.where(
                        UserSpecification.notDeleted()
                )

                .and(UserSpecification.keyword(keyword))

                .and(UserSpecification.hasId(id))

                .and(UserSpecification.firstName(firstName))
                .and(UserSpecification.lastName(lastName))
                .and(UserSpecification.username(username))
                .and(UserSpecification.email(email))
                .and(UserSpecification.phoneNo(phoneNo))

                .and(UserSpecification.role(role))
                .and(UserSpecification.status(status))

                .and(UserSpecification.emailVerified(emailVerified))
                .and(UserSpecification.accountNonLocked(accountNonLocked))

                .and(UserSpecification.createdAfter(createdAfter))
                .and(UserSpecification.createdBefore(createdBefore));
    }
}
