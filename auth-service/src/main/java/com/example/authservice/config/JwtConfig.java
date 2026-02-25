package com.example.authservice.config;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Base64;

public class JwtConfig {
    @Value("${jwt.secret}")
    private String secret;
    @Bean
    public SecretKey secretKey(){
        byte[] keyBytes = Base64.getEncoder().encode(secret.getBytes());
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
