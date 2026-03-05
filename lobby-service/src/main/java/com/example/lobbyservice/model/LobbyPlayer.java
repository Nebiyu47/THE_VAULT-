package com.example.lobbyservice.model;

import com.example.lobbyservice.enums.PlayerRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LobbyPlayer implements Serializable {

    private UUID playerId;

    private String username;

    private String avatarUrl;

    private PlayerRole role;

    private boolean isReady;

    private LocalDateTime joinedAt;

    private String connectionId; // WebSocket session ID

    private boolean isOnline;

    private PlayerStats stats;

}
