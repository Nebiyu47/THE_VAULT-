package com.example.apigateway.fallback;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    // Using @RequestMapping handles GET, POST, PUT, etc.
    // This is critical for Registration (POST) and Login (POST)

    @RequestMapping("/auth")
    public ResponseEntity<Map<String, Object>> authFallback() {
        return fallbackResponse("Authentication Service is currently offline. The Vault is locked.");
    }

    @RequestMapping("/lobby")
    public ResponseEntity<Map<String, Object>> lobbyFallback() {
        return fallbackResponse("Lobby Service is currently unavailable. Social systems offline.");
    }

    @RequestMapping("/game")
    public ResponseEntity<Map<String, Object>> gameFallback() {
        return fallbackResponse("Game Service is currently unavailable. Matchmaking suspended.");
    }

    @RequestMapping("/chat")
    public ResponseEntity<Map<String, Object>> chatFallback() {
        return fallbackResponse("Chat Service is currently unavailable. Communication scrambled.");
    }

    @RequestMapping("/payment")
    public ResponseEntity<Map<String, Object>> paymentFallback() {
        return fallbackResponse("Transaction Service is currently unavailable. Credits frozen.");
    }

    /**
     * Helper to build a consistent JSON error response for the Angular frontend.
     */
    private ResponseEntity<Map<String, Object>> fallbackResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("error", "Service Unavailable");
        response.put("message", message);
        response.put("path", "Circuit Breaker Fallback");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}