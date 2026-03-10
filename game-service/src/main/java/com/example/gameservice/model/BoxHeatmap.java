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
public class BoxHeatmap {

    private int position;
    private int hoverCount;
    private Map<UUID,Integer> playerHovers;
    private double intensity;

}

