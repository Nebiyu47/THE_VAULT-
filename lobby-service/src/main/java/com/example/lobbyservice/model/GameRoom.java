package com.example.lobbyservice.model;

import com.example.lobbyservice.enums.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "rooms")
@RedisHash("Room")
public class GameRoom implements Serializable {

    @Id
    private String id;

    @Indexed
    private String roomCode;

    private String name;

    @Indexed
    private RoomStatus status;

    @Indexed
    private UUID architectId;

    private String architectName;

    @Builder.Default
    private List<LobbyPlayer> players = new ArrayList<>();

    @Builder.Default
    private int maxPlayers = 8;

    @Builder.Default
    private int minPlayers = 2;

    private boolean isPrivate;

    private String password;

    private GameConfig gameConfig;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime startedAt;

    private String gameId; // Reference to active game

    @Builder.Default
    private List<String> chatHistory = new ArrayList<>();

    @Builder.Default
    private List<String> bannedPlayers = new ArrayList<>();


}
