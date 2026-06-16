package com.example.auth_service.repository.specification;

import com.example.auth_service.entity.User;
import com.example.auth_service.enums.AccountStatus;
import com.example.auth_service.enums.Role;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserSpecification {

    public static Specification<User> keyword(
            String keyword
    ) {
        return (root, query, cb) -> {

            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }

            String value = "%" + keyword + "%";

            return cb.or(

                    cb.like(
                            cb.lower(root.get("firstName")),
                            value.toLowerCase()
                    ),
                    cb.like(
                            cb.lower(root.get("lastName")),
                            value.toLowerCase()
                    ),
                    cb.like(
                            cb.lower(root.get("username")),
                            value.toLowerCase()
                    ),
                    cb.like(
                            cb.lower(root.get("email")),
                            value.toLowerCase()
                    ),
                    cb.like(
                            cb.lower(root.get("phoneNo")),
                            value.toLowerCase()
                    ),
                    cb.like(
                            cb.lower(root.get("status")),
                            value.toUpperCase()
                    ),
                    cb.like(
                            cb.lower(root.get("role")),
                            value.toUpperCase()
                    ),
                    cb.like(
                            cb.lower(root.get("institution")),
                            value.toLowerCase()
                    ),
                    cb.like(
                            cb.lower(root.get("faculty")),
                            value.toLowerCase()
                    ),
                    cb.like(
                            cb.lower(root.get("department")),
                            value.toLowerCase()
                    )
            );
        };
    }

    public static Specification<User> hasId(
            Long id
    ) {
        return (root, query, cb) ->

                id == null
                        ? cb.conjunction()
                        : cb.equal(root.get("id"), id);
    }

    public static Specification<User> firstName(
            String firstName
    ) {
        return (root, query, cb) ->

                firstName == null || firstName.isBlank()
                        ? cb.conjunction()
                        : cb.like(
                        cb.lower(root.get("firstName")),
                        "%" + firstName.toLowerCase() + "%"
                );
    }

    public static Specification<User> lastName(
            String lastName
    ) {
        return (root, query, cb) ->

                lastName == null || lastName.isBlank()
                        ? cb.conjunction()
                        : cb.like(
                        cb.lower(root.get("lastName")),
                        "%" + lastName.toLowerCase() + "%"
                );
    }

    public static Specification<User> username(
            String username
    ) {
        return (root, query, cb) ->

                username == null || username.isBlank()
                        ? cb.conjunction()
                        : cb.like(
                        cb.lower(root.get("username")),
                        "%" + username.toLowerCase() + "%"
                );
    }

    public static Specification<User> email(
            String email
    ) {
        return (root, query, cb) ->

                email == null || email.isBlank()
                        ? cb.conjunction()
                        : cb.like(
                        cb.lower(root.get("email")),
                        "%" + email.toLowerCase() + "%"
                );
    }

    public static Specification<User> phoneNo(
            String phoneNo
    ) {
        return (root, query, cb) ->

                phoneNo == null || phoneNo.isBlank()
                        ? cb.conjunction()
                        : cb.like(
                        root.get("phoneNo"),
                        "%" + phoneNo + "%"
                );
    }

    public static Specification<User> role(
            Role role
    ) {
        return (root, query, cb) ->

                role == null
                        ? cb.conjunction()
                        : cb.equal(
                        root.get("role"),
                        role
//                        "%" + role.toString().toUpperCase() + "%"
                );
    }

    public static Specification<User> status(
            AccountStatus status
    ) {
        return (root, query, cb) ->

                status == null
                        ? cb.conjunction()
                        : cb.equal(
                        root.get("status"),
                        status
                );
    }

    public static Specification<User> emailVerified(
            Boolean emailVerified
    ) {
        return (root, query, cb) ->

                emailVerified == null
                        ? cb.conjunction()
                        : cb.equal(
                        root.get("emailVerified"),
                        emailVerified
                );
    }

    public static Specification<User> accountNonLocked(
            Boolean accountNonLocked
    ) {
        return (root, query, cb) ->

                accountNonLocked == null
                        ? cb.conjunction()
                        : cb.equal(
                        root.get("accountNonLocked"),
                        accountNonLocked
                );
    }

    public static Specification<User> createdAfter(
            LocalDateTime createdAfter
    ) {
        return (root, query, cb) ->

                createdAfter == null
                        ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(
                        root.get("registeredAt"),
                        createdAfter
                );
    }

    public static Specification<User> createdBefore(
            LocalDateTime createdBefore
    ) {
        return (root, query, cb) ->

                createdBefore == null
                        ? cb.conjunction()
                        : cb.lessThanOrEqualTo(
                        root.get("registeredAt"),
                        createdBefore
                );
    }

    public static Specification<User> notDeleted() {

        return (root, query, cb) ->
                cb.notEqual(
                        root.get("status"),
                        AccountStatus.DELETED
                );
    }
}