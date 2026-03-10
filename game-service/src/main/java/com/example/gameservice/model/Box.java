package com.example.gameservice.model;

import com.example.gameservice.model.enums.BoxType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Box {
    private String boxId;
    private int position;
    private int row;
    private int col;
    private BoxType type;
    private boolean isOpened;
    private UUID openedBy;
    private LocalDateTime openedAt;
    private int value;
    @Builder.Default
    private int heatmapValue=0;
    @Builder.Default
    private int hoverCount=0;
    private List<UUID> hoveredBy;

}
