package com.example.lobbyservice.model;

import com.example.lobbyservice.enums.Difficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameConfig implements Serializable {
    private int gridSize=5;
    private int jackpotCount = 3;
    private int trapCount = 5;
    private int emptyCount = 17 ;
    private int entryFee= 100 ;
    private int greedTimerSeconds = 30 ;
    private boolean allowTaunts = true;
    private boolean showHeatmap = true;
    private Difficulty difficulty = Difficulty.MEDIUM;

}
