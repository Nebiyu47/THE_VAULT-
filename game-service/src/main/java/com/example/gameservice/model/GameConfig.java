package com.example.gameservice.model;

import com.example.gameservice.model.enums.Difficulty;
import com.example.gameservice.model.enums.TrapPlacement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class GameConfig {
    private int gridSize =5;
    private int jackpotCount= 3;
    private int trapCount=5;
    private int emptyCount=17;
    private int entryFee = 10;
    private int baseJackpot=50;
    private int jackpotIncrement=5;
    private int greedTimerSeconds=30;
    private boolean allowHeatmap=true;
    private boolean allowTaunts = true;
    private Difficulty difficulty = Difficulty.MEDIUM;
    private TrapPlacement trapPlacement = TrapPlacement.RANDOM;

}
