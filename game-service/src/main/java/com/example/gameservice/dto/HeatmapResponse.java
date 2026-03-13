package com.example.gameservice.dto;

import com.example.gameservice.model.PlayerHeatmap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapResponse {

   private UUID gameId;
   private Map<Integer , Integer> boxHoverCounts;
   private Map<Integer,Double> boxIntensities;
   private List<PlayerHeatmap>playerHeatmaps;
   private long totalHovers;
     @Data
     @Builder
     @NoArgsConstructor
     @AllArgsConstructor
      public static class PlayerHeatmap{
         private UUID playerId;
         private String username;
         private Map<Integer,Integer>boxHovers;
         private int totalHovers;
     }
}
