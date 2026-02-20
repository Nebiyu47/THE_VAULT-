package com.example.authservice.repository;

import com.example.authservice.model.RefreshToken;
import com.example.authservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {


    Optional<RefreshToken> findByToken(String token);
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.revoked=true WHERE rt.user.id= :userId")
    void revokeAllUserTokens(@Param("userId")UUID userId);
    @Modifying
    @Transactional
    void deleteByExpiresAtBefore(LocalDateTime now);
    List<RefreshToken> findAllByByUserAndRevokedFalse(User user);
}
