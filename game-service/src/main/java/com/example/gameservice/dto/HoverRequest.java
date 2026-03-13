package com.example.gameservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HoverRequest {

  private UUID gameId;
  private UUID playerId;
  private int boxPosition;
  private String action;
}
