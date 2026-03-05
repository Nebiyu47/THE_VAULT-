package com.example.lobbyservice.dto;

import com.example.lobbyservice.enums.RoomStatus;
import com.example.lobbyservice.model.GameConfig;
import com.example.lobbyservice.model.GameRoom;
import com.example.lobbyservice.model.LobbyPlayer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {

    private String id;
    private String roomCode;
    private String name;
    private RoomStatus status;
    private UUID architectId;
    private String architectName;
    private List<LobbyPlayer> players;
    private int maxPlayers;
    private int minPlayers;
    private boolean isPrivate;
    private GameConfig gameConfig;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private String gameId;

    public static RoomResponse fromGameRoom(GameRoom room) {
        return RoomResponse.builder()
                .id(room.getId())
                .roomCode(room.getRoomCode())
                .name(room.getName())
                .status(room.getStatus())
                .architectId(room.getArchitectId())
                .architectName(room.getArchitectName())
                .players(room.getPlayers())
                .maxPlayers(room.getMaxPlayers())
                .minPlayers(room.getMinPlayers())
                .isPrivate(room.isPrivate())
                .gameConfig(room.getGameConfig())
                .createdAt(room.getCreatedAt())
                .startedAt(room.getStartedAt())
                .gameId(room.getGameId())
                .build();
    }
}