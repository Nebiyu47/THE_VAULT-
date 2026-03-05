package com.example.authservice.controller;

import com.example.authservice.dto.*;
import com.example.authservice.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j

public class AuthController {

    private final AuthService authService;
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
            ){
        log.info("Post /api/auth/register  - Registrting user: {}", request.getUsername());
        AuthResponse response = authService.register(request,httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request ,
            HttpServletRequest httpRequest
            ){
        log.info("POST /api/auth/login - Login attempt for: {}", request.getUsernameOrEmail());
      AuthResponse response = authService.login(request,httpRequest);
      return ResponseEntity.ok(response);
    }
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse>refreshToken(@Valid @RequestBody RefreshTokenRequest request
    ,HttpServletRequest httpRequest){
        log.info("POST /api/auth/refresh - Refreshing token");
        AuthResponse response = authService.refreshToken(request,httpRequest);
        return ResponseEntity.ok(response);
    }
    @PostMapping("/logout")
    public ResponseEntity<Void>logout(
            @RequestHeader("Authorization")String token, HttpServletRequest request
    ){
        log.info("POST /api/auth/logout - Logging out user");
        authService.logout(token,request);
        return ResponseEntity.ok().build();
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword (@Valid @RequestBody PasswordResetRequest request){
        log.info("POST /api/auth/forgot-password - Password reset requested for: {}", request.getEmail());
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok().build();
    }
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetConfirm resetConfirm){
        log.info("POST /api/auth/reset-password - Resetting password");
        authService.resetPassword(resetConfirm);
        return ResponseEntity.ok().build();
    }
    @GetMapping("/verify")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        log.info("GET /api/auth/verify - Verifying email with token");
        authService.verifyEmail(token);
        return ResponseEntity.ok().build();
    }
   @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getCurrentUser(
           @AuthenticationPrincipal UserDetails userDetails
           ){
       log.info("GET /api/auth/me - Getting current user profile");
       String username = userDetails.getUsername();
       // You'll need to implement a method to get user by username
       // You'll need to implement a method to get user by username
       return ResponseEntity.ok(authService.getUserProfile(null)); // Fix this
   }
   @GetMapping("/profile/{userId}")
    public ResponseEntity<UserProfileDTO> getUseerProfile(@PathVariable UUID userId){
       log.info("GET /api/auth/profile/{} - Getting user profile", userId);
       UserProfileDTO userProfileDTO = authService.getUserProfile(userId);
       return ResponseEntity.ok(userProfileDTO);
   }
   @PutMapping("/profile")
    public ResponseEntity<UserProfileDTO>updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserProfileDTO userProfileDTO
   ){
       log.info("PUT /api/auth/profile - Updating user profile");
       // Get user ID from authentication
       // You'll need to implement this properly
       return ResponseEntity.ok(authService.updatedProfile(null, userProfileDTO));

   }
}
