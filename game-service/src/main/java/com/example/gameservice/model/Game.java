package com.example.gameservice.model;

import com.example.gameservice.model.enums.GameStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "games")
@CompoundIndex(def = "{'roomCode': 1, 'status': 1}")
public class Game {

    @Id
    private String id;

    @Indexed(unique = true)
    private UUID gameId;

    @Indexed
    private String roomCode;

    private UUID roomId;

    @Indexed
    private GameStatus status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private UUID architectId;

    private String architectName;

    @Builder.Default
    private List<GamePlayer> players = new ArrayList<>();

    @Builder.Default
    private GameConfig config = new GameConfig();

    private int jackpotAmount;

    @Builder.Default
    private int currentJackpot = 0;

    private int totalBoxes;

    private int openedBoxes;

    private UUID winnerId;

    private String winnerName;

    @Builder.Default
    private List<Box> boxes = new ArrayList<>();

    @Builder.Default
    private List<Move> moves = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> heatmapData = new HashMap<>();

    private LocalDateTime greedTimerStarted;

    private int greedTimerSeconds;

    private boolean isGreedTimerActive;

    private LocalDateTime lastActivity;
}
