package com.example.auth_service.repository;

import com.example.auth_service.entity.PasswordResetToken;
import com.example.auth_service.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUser(User user);

//    @Modifying
//    @Transactional
//    @Query("""
//    DELETE FROM PasswordResetToken p
//    WHERE p.expiryDate < :now
//""")
//    void deleteExpired(
//            @Param("now")
//            LocalDateTime now
//    );

    @Transactional
    void deleteByExpiryDateBefore(
            LocalDateTime now
    );
}