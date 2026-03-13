package com.example.gameservice.dto;

import com.example.gameservice.model.enums.BoxType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MoveResponse {
    private UUID playerId;
    private String playerName;
    private int position;
    private BoxType type;
    private LocalDateTime timestamp;
    private int pointsEarned;
    private boolean isWinningMove;


}
