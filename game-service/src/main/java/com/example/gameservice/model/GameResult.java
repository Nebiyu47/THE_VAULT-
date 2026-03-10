package com.example.gameservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "results")
public class GameResult {
    @Id
    private String id;
    private UUID resultId;
    @Indexed
    private UUID gameId;
    private String roomCode;
    private UUID winnerName;
    private int jackpotWon;
    private int totalMoves;
    private int boxesOpened;
    private LocalDateTime completedAt;
    private int gameDurationSeconds;
    private List<PlayerResult> playerResult;
    private List<UUID> trapVictims;
    private Map<String,Object>replayData;
    private Map<String,Object>statistics;
}
