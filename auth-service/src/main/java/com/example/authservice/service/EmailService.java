package com.example.authservice.service;

import com.example.authservice.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final RedisTemplate<String,Object>redisTemplate;
    @Value("${spring.mail.username}")
    private String fromEmail;
    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;
    @Async
    public void sendVerificationEmail(User user) {
        try {
            String token = UUID.randomUUID().toString();
            String verificationKey = "email:verify:" + token;
            redisTemplate.opsForValue().set(verificationKey, user.getId().toString(), 24, TimeUnit.HOURS);

            String verificationLink = frontendUrl + "/auth/verify?token=" + token;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Welcome to THE VAULT - Verify Your Email");
            message.setText(buildVerificationEmail(user.getUsername(), verificationLink));

            mailSender.send(message);
            log.info("Verification email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email: {}", e.getMessage());
        }
    }
    @Async
    public void sendPasswordResetEmail(User user , String token){
        try {
            String resetLink = frontendUrl + "/auth/reset-password?token"+token;
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setSubject("The Vault - password reset password");
            message.setText(buildPasswordResetEmail(user.getUsername(),resetLink));
            mailSender.send(message);
            log.info("Password reset email sent to :{}", user.getEmail());


        }catch (Exception e){
            log.error("faild to send password reset email:",e.getMessage());
        }

    }
    private String buildVerificationEmail(String username, String link) {
        return String.format("""
            Hello %s,
            
            Welcome to THE VAULT! Please verify your email address by clicking the link below:
            
            %s
            
            This link will expire in 24 hours.
            
            If you didn't create an account, please ignore this email.
            
            See you in The Vault!
            """, username, link);
    }
    private String buildPasswordResetEmail(String username , String link){
        return  String.format("""
            Hello %s,
            
            We received a request to reset your password. Click the link below to reset it:
            
            %s
            
            This link will expire in 1 hour.
            
            If you didn't request a password reset, please ignore this email or contact support.
            
            - The Vault Team
            """, username, link);
    }
    private String buildWelcomeEmail(String username) {
        return String.format("""
            Hello %s,
            
            Welcome to THE VAULT! Your account has been successfully verified.
            
            You've been credited with 1000 VAULT POINTS to start your journey.
            
            Ready to test your luck? Join a game now and see if you have what it takes to beat the GREED TIMER!
            
            Game Features:
            • Play as Breacher - Open boxes, avoid traps, claim the jackpot!
            • Play as Architect - Design trap layouts and outsmart other players
            • Real-time chat with taunts and reactions
            • Earn achievements and climb the leaderboard
            • Build your streak and earn bonus points
            
            Start playing: %s/lobby
            
            May the odds be ever in your favor!
            - The Vault Team
            """, username, frontendUrl);
    }
}
