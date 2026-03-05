package com.example.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;

@Component
@Slf4j
public class JwtAuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<JwtAuthenticationGatewayFilterFactory.Config> {

    @Value("${jwt.secret}")
    private String secret;

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public JwtAuthenticationGatewayFilterFactory(ReactiveRedisTemplate<String, String> redisTemplate) {
        super(Config.class);
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        log.info("JwtAuthenticationGatewayFilterFactory bean initialized!");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Skip auth for certain paths
            String path = request.getURI().getPath();
            if (path.startsWith("/api/auth/") || path.startsWith("/ws/") || path.startsWith("/fallback/")) {
                return chain.filter(exchange);
            }

            // Extract token
            String authHeader = request.getHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Missing or invalid Authorization header for path: {}", path);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String token = authHeader.substring(7);

            try {
                // Check if token is blacklisted
                return redisTemplate.hasKey("blacklist:" + token)
                        .flatMap(isBlacklisted -> {
                            if (Boolean.TRUE.equals(isBlacklisted)) {
                                log.warn("Blacklisted token used for path: {}", path);
                                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                                return exchange.getResponse().setComplete();
                            }

                            // Validate JWT - FIX THE KEY ENCODING
                            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes()); // Don't double-encode
                            Claims claims = Jwts.parserBuilder()
                                    .setSigningKey(key)
                                    .build()
                                    .parseClaimsJws(token)
                                    .getBody();

                            // Add user info to headers
                            String userId = claims.get("userId", String.class);
                            if (userId == null) {
                                userId = claims.getSubject();
                            }
                            String username = claims.getSubject();
                            String role = claims.get("role", String.class);
                            if (role == null) {
                                role = "USER";
                            }

                            ServerHttpRequest mutatedRequest = request.mutate()
                                    .header("X-User-ID", userId != null ? userId : "")
                                    .header("X-Username", username != null ? username : "")
                                    .header("X-User-Role", role)
                                    .build();

                            return chain.filter(exchange.mutate().request(mutatedRequest).build());
                        });

            } catch (Exception e) {
                log.error("JWT validation failed: {}", e.getMessage());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        };
    }

    public static class Config {
        // Configuration properties if needed
    }
}