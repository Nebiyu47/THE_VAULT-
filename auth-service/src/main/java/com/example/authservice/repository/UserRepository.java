package com.example.authservice.repository;

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
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User>findByEmail(String email);
    Optional<User>findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    @Query("SELECT u FROM User u where u.email =:email OR u.username= :username")
    Optional<User> findByEmailOrUsername(@Param("email")String email , @Param("username") String username);
     @Modifying
     @Transactional
     @Query("UPDATE User u SET u.lastLogin =:lastLogin WHERE u.id =:userId")
     void updatedLastLogin(@Param("userId")UUID userId , @Param("lastLogin")LocalDateTime lastLogin);
     @Modifying
     @Transactional
     @Query("UPDATE User u SET u.vaultPoints =u.vaultPoints+:points WHERE u.id=:userId")
     void addVaultPoints(@Param("userId")UUID userId, @Param("points") int points);
     @Query("select u from User u where u.isVerified = false AND u.createdAt<:cutoffTime")
     List<User> findUnverifiedUsersCreatedBefore(@Param("cutoffTime")LocalDateTime cutoffTime);

}
