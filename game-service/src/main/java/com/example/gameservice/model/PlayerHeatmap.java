package com.example.gameservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlayerHeatmap {

 private UUID playerId;
 private String username;
 private Map<Integer,Integer>boxHovers;
 private int totalHovers;
 private double[] focusArea;
}
