package com.example.gameservice.model;

import com.example.gameservice.model.enums.BoxType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "moves")
public class Move {
    @Id
    private String id;
    private UUID moveId;
    @Indexed
    private UUID gameId;
    private UUID playerId;
    private String playerName;
    private String boxId;
    private int position;
    private BoxType boxType;
    private LocalDateTime timestamp;
    private int pointsEarned;
    private boolean isWinningMove;
    private int jackpotAmount;
    private Map<String,Object>metadata;
}
