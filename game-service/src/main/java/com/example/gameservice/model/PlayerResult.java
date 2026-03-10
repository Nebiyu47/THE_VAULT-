package com.example.gameservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerResult {

  private UUID playerId;
  private String username;
  private int boxesOpened;
  private int pointsEarned;
  private int trapsTriggered;
  private int jackpotsFound;
  private boolean isWinner;
  private int rank;
}
