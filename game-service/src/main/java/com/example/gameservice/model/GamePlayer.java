package com.example.gameservice.model;

import com.example.gameservice.model.enums.PlayerRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GamePlayer {

  private UUID playerId;
  private String username;
  private String avatarUrl;
  private PlayerRole role;
  private boolean isActive;
  private int boxesOpened;
  private int pointsEarned;
  private LocalDateTime lastMoveAt;
  private String currentHoverPosition;

}
