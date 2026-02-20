package com.example.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private UUID userId;
    private String username;
    private String email;
    private String role;
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private String tokenType="Bearer";
}
