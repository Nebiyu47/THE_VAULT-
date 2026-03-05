package com.example.lobbyservice.dto;

import com.example.lobbyservice.enums.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LobbyUpdateEvent {
    private String eventType;
    private String roomCode;
    private RoomStatus status;
    private List<PlayerResponse>players;
    private int readyCount;
    private int countdown;
    private UUID updatedAt;
    private LocalDateTime timestamp;

}
