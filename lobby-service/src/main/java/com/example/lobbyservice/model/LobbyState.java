package com.example.lobbyservice.model;

import com.example.lobbyservice.enums.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "lobby_states")
public class LobbyState implements Serializable {
    private String id;
    private String roomCode;
    private RoomStatus status;
    private int playerCount;
    private int readyCount;
    private int countdown;
    private boolean allPlayersReady;
    @TimeToLive(unit = TimeUnit.SECONDS)
    private Long ttl = 300L;
}
