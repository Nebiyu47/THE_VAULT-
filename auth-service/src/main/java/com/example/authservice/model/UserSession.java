package com.example.authservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="user_session")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;
    @Column(name = "session_token",unique = true,nullable = false,length = 500)
    private String sessionToken;
    @Column(name = "ip_address")
    private InetAddress ipAddress;
    @Column(name = "user_agent")
    private String userAgent;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    @Column(name = "is_active")
    private boolean isActive = true;

}
