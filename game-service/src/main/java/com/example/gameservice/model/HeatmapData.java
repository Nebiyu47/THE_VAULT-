package com.example.gameservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapData {
    private UUID gameId;
    private Map<Integer,BoxHeatmap> boxHeatmapMap;
    private Map<UUID,PlayerHeatmap> playerHeatmapMap;
    private long totalHovers;

}
