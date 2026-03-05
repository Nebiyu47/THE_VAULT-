package com.example.lobbyservice.dto;

import com.example.lobbyservice.enums.PlayerRole;
import com.example.lobbyservice.model.LobbyPlayer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlayerResponse {
    private UUID playerId;
    private String username;
    private String avatarUrl;
    private PlayerRole role;
    private boolean isReady;
    private LocalDateTime joinedAt;
    private boolean isOnline;
    public static  PlayerResponse fromLobbyPlayer(LobbyPlayer player){
        return PlayerResponse.builder()
                .playerId(player.getPlayerId())
                .username(player.getUsername())
                .avatarUrl(player.getAvatarUrl())
                .role(player.getRole())
                .isReady(player.isReady())
                .joinedAt(player.getJoinedAt())
                .isOnline(player.isOnline())
                .build();
    }
}
