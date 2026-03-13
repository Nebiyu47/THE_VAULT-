package com.example.gameservice.dto;

import com.example.gameservice.model.enums.BoxType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoxResultResponse {
    private String boxId;
    private int position;
    private int row;
    private int col;
    private BoxType type;
    private boolean isOpened;
    private UUID openedBy;
    private String openedByName;
    private int value;
    private int newJackpot;
    private boolean isGameEnded;
    private UUID winnerId;
    private String winnerName;
    private String message;
}
