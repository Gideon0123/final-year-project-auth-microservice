package com.example.auth_service.repository;

import com.example.auth_service.entity.User;
import com.example.auth_service.enums.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

//    findAll(spec, pageable);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByPhoneNo(String phoneNo);

    Optional<User> findByIdAndStatusNot(Long id, AccountStatus status);

    Page<User> findAllByStatusNot(AccountStatus status, Pageable pageable);

    List<User> findByStatusAndSuspendedUntilBefore(
            AccountStatus status, LocalDateTime date
    );

    boolean existsByUsernameAndIdNot(String username, Long id);
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByPhoneNoAndIdNot(String phoneNo, Long id);
}