package com.example.auth_service.repository;

import com.example.auth_service.entity.EmailVerificationToken;
import com.example.auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);
    void deleteByUser(User user);

    Optional<EmailVerificationToken> findByUser(User user);

    @Modifying
    @Query("""
DELETE FROM EmailVerificationToken t
WHERE t.expiryDate < :now
""")
    int deleteExpired(
            @Param("now") LocalDateTime now
    );

    @Modifying
    @Query("""
       DELETE FROM EmailVerificationToken e
       WHERE e.user.id = :userId
       """)
    void deleteByUserId(Long userId);
}