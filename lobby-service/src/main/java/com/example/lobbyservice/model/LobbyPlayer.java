package com.example.lobbyservice.model;
import org.springframework.data.annotation.Id;
import com.example.lobbyservice.enums.PlayerRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("players")
public class LobbyPlayer implements Serializable {
    @Id
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
