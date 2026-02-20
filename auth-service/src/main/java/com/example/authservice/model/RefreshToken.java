package com.example.authservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name ="refresh_tokens" )
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {
    @Id
    @GeneratedValue
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;
    @Column(unique = true,nullable = false,length = 500)
    private String token;
    @Column(name = "expires_at",nullable = false)
    private LocalDateTime expiresAt;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    private boolean revoked = false;
    @Column(name = "device_info ",columnDefinition = "jsonb")
    private String deviceInfo;
}
