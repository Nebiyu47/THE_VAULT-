package com.example.authservice.service;

import com.example.authservice.dto.*;
import com.example.authservice.model.RefreshToken;
import com.example.authservice.model.Role;
import com.example.authservice.model.User;
import com.example.authservice.model.UserSession;
import com.example.authservice.repository.RefreshTokenRepository;
import com.example.authservice.repository.UserRepository;
import com.example.authservice.repository.UserSessionRepository;
import com.example.authservice.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private final EmailService emailService;
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        log.info("Registering new user with email: {}", request.getEmail());
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("email already is registerd");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("user name is already existed");
        }
        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(request.getRole() != null ? Role.valueOf(request.getRole()) : Role.BREACHER)
                .isActive(true)
                .isVerified(false)
                .vaultPoints(1000)
                .createdAt(LocalDateTime.now())
                .build();
        user = userRepository.save(user);
        UserDetails userDetails = loadUserByUsername(user.getUsername());
        String newAccessToken = jwtService.generateToken(userDetails, user.getId(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(userDetails, user.getId());
        saveRefreshToken(user, refreshToken, httpRequest);
        createUserSession(user, httpRequest);
        emailService.sendVerificationEmail(user);
        String userKey = "user:" + user.getId();
        redisTemplate.opsForValue().set(userKey, user, 1, TimeUnit.HOURS);
        log.info("User registered successfully: {}", user.getUsername());
        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtExpiration)
                .tokenType("Bearer")
                .build();

    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        log.info("Login attempt for user: {}", request.getUsernameOrEmail());

        // Authenticate
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Get user
        User user = userRepository.findByEmailOrUsername(
                request.getUsernameOrEmail(),
                request.getUsernameOrEmail()
        ).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Generate tokens
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String accessToken = jwtService.generateToken(userDetails, user.getId(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(userDetails, user.getId());

        // Save refresh token
        if (!request.isRememberMe()) {
            // Set shorter expiration for refresh token if not remember me
            refreshToken = jwtService.generateRefreshToken(userDetails, user.getId());
        }
        saveRefreshToken(user, refreshToken, httpRequest);

        // Create session
        createUserSession(user, httpRequest);

        // Update Redis cache
        String userKey = "user:" + user.getId();
        redisTemplate.opsForValue().set(userKey, user, 1, TimeUnit.HOURS);

        // Track online user
        redisTemplate.opsForSet().add("online:users", user.getId().toString());

        log.info("User logged in successfully: {}", user.getUsername());

        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtExpiration)
                .tokenType("Bearer")
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        String refreshToken = request.getRefreshToken();
        RefreshToken tokenEntity = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Invalid refresh Token"));
        if (tokenEntity.isRevoked() || tokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("refresh token is expired or revoked");
        }
        User user = tokenEntity.getUser();
        UserDetails userDetails = loadUserByUsername(user.getUsername());
        String newAccessToken = jwtService.generateToken(userDetails, user.getId(), user.getRole().name());
        String newRefreshToken = jwtService.generateRefreshToken(userDetails, user.getId());
        //Revoked old refresh token
        tokenEntity.setRevoked(true);
        refreshTokenRepository.save(tokenEntity);
        saveRefreshToken(user, newRefreshToken, httpRequest);
        log.info("Token refreshed for user: {}", user.getUsername());
        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtExpiration)
                .build();
    }

    @Transactional
    public void logout(String token, HttpServletRequest httpRequest) {
        try {
            String jwt = token.substring(7);
            UUID userId = jwtService.extractUserId(jwt);
            if (userId != null) {
                refreshTokenRepository.revokeAllUserTokens(userId);
                userSessionRepository.deactivateAllUserSessions(userId);
                String blacklistKey = "blacklist:" + jwt;
                long expiration = jwtService.extractExpiration(jwt).getTime() - System.currentTimeMillis();
                redisTemplate.opsForValue().set(blacklistKey, "revoked", expiration, TimeUnit.MILLISECONDS);
                redisTemplate.opsForSet().remove("online:users", userId.toString());
                redisTemplate.delete("user:" + userId);
                log.info("user logout successfully");
            }


        } catch (Exception e) {
            log.error("Error during logout:{}", e.getMessage());
        }
    }

    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        // Generate reset token
        String resetToken = UUID.randomUUID().toString();
        // store in redis with expiration
        String resetKey = "password:reset" + resetToken;
        redisTemplate.opsForValue().set(resetKey, user.getId().toString(), 1, TimeUnit.HOURS);
        emailService.sendPasswordResetEmail(user, resetToken);
        log.info("password reset requested for user:{} ", user.getUsername());
    }

    @Transactional
    public void resetPassword(PasswordResetConfirm request) {
        String resetKey = "password:reset:" + request.getToken();
        String userIdStr = (String) redisTemplate.opsForValue().get(resetKey);
        if (userIdStr == null) {
            throw new RuntimeException("Invalid or expired reset token");
        }
        UUID userId = UUID.fromString(userIdStr);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        redisTemplate.delete(resetKey);
        refreshTokenRepository.revokeAllUserTokens(userId);
        userSessionRepository.deactivateAllUserSessions(userId);
        log.info("password reset successful for user: {} ", user.getUsername());

    }

    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfile(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("user not found"));
        return mapToProfileDto(user);
    }

    @Transactional
    public UserProfileDTO updatedProfile(UUID userId, UserProfileDTO profileDTO) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("user not found"));
        if (profileDTO.getFullName() != null) {
            user.setFullName(profileDTO.getFullName());
        }
        if (profileDTO.getAvatarUrl() != null) {
            user.setAvatarUrl(profileDTO.getAvatarUrl());
        }
        if (profileDTO.getPreferences() != null) {
            user.setPreferences(profileDTO.getPreferences());
        }
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);
        String userKey = "user:" + userId;
        redisTemplate.opsForValue().set(userKey, user, 1, TimeUnit.HOURS);
        log.info("profile updated for user: {} ", user.getUsername());
        return mapToProfileDto(user);

    }

    @Transactional
    public void verifyEmail(String token) {
        String verifyKey = "email:verify:" + token;
        String userIdStr = (String) redisTemplate.opsForValue().get(verifyKey);
        if (userIdStr == null) {
            throw new RuntimeException("Invalid or expired verification token");
        }
        UUID userId = UUID.fromString(userIdStr);
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        redisTemplate.delete(verifyKey);
        log.info("Email verified for user: {}", user.getUsername());
    }

    private void saveRefreshToken(User user, String token, HttpServletRequest request) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .createdAt(LocalDateTime.now())
                .revoked(false)
                .deviceInfo(createDeviceInfo(request))
                .build();
    }

    private void createUserSession(User user, HttpServletRequest request) {
        UserSession userSession = UserSession.builder()
                .user(user)
                .sessionToken(UUID.randomUUID().toString())
                .ipAddress(getClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().minusHours(24))
                .isActive(true)
                .build();
    }

    private InetAddress getClientIp(HttpServletRequest request) {
        try {
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) {
                ip = request.getRemoteAddr();
            }
            return InetAddress.getByName(ip.split(",")[0].trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String createDeviceInfo(HttpServletRequest request) {
        return String.format("{\"userAgent\": \"%s\"}", request.getHeader("User-Agent"));

    }

    private UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usesr not Found"));
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                user.isActive(),
                true,
                true,
                !user.isVerified(),
                Collections.singletonList(new SimpleGrantedAuthority("Role_" + user.getRole().name()))
        );
    }

    private UserProfileDTO mapToProfileDto(User user) {
        double winRate = user.getTotalWins() > 0
                ? (double) user.getTotalWins() / user.getTotalGames() * 100 : 0;
        return UserProfileDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name())
                .isVerified(user.isVerified())
                .vaultPoints(user.getVaultPoints())
                .totalWins(user.getTotalWins())
                .totalGames(user.getTotalGames())
                .winRate(winRate)
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .preferences(user.getPreferences())
                .build();
    }
}
