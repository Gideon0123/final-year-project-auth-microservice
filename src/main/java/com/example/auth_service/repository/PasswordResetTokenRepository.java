package com.example.auth_service.repository;

import com.example.auth_service.entity.PasswordResetToken;
import com.example.auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUser(User user);

    @Modifying
    @Query("""
DELETE FROM PasswordResetTokenRepository p
WHERE p.expiryDate < :now
""")
    int deleteExpired(
            @Param("now") LocalDateTime now
    );
}