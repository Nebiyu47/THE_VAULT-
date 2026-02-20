package com.example.authservice.repository;

import com.example.authservice.model.User;
import com.example.authservice.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession , UUID> {
    Optional<UserSession> findBySessionToken(String token);
    @Modifying
    @Transactional
    @Query("UPDATE UserSession us SET us.isActive=false WHERE us.user.id=:userId")
    void deactivateAllUserSessions(@Param("userId")UUID userId);
    @Modifying
    @Transactional
    void deleteByExpiresAtBefore(LocalDateTime now);
    long countByUserAndIsActiveTrue(User user);

}
