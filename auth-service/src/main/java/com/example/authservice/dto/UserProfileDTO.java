package com.example.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
 private UUID id;
 private String username;
 private String email;
 private String fullName;
 private String avatarUrl;
 private String role;
 private boolean isVerified;
 private int vaultPoints;
 private int totalWins;
 private int totalGames;
 private double winRate;
 private LocalDateTime createdAt;
 private LocalDateTime lastLogin;
 private String preferences;
}
