package com.example.gameservice.service;

import com.example.gameservice.model.Box;
import com.example.gameservice.model.GameConfig;
import com.example.gameservice.model.enums.BoxType;
import com.example.gameservice.model.enums.Difficulty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class BoxService {

    public List<Box> initializeBoxes(int gridSize, int jackpotCount, int trapCount, Difficulty difficulty) {
        int totalBoxes = gridSize * gridSize;
        int emptyCount = totalBoxes - jackpotCount - trapCount;

        // Adjust trap count based on difficulty
        int adjustedTrapCount = (int) (trapCount * difficulty.trapMultiplier);
        adjustedTrapCount = Math.min(adjustedTrapCount, totalBoxes - jackpotCount);

        // Create box types list
        List<BoxType> types = new ArrayList<>();
        for (int i = 0; i < jackpotCount; i++) types.add(BoxType.JACKPOT);
        for (int i = 0; i < adjustedTrapCount; i++) types.add(BoxType.TRAP);
        for (int i = 0; i < totalBoxes - jackpotCount - adjustedTrapCount; i++) types.add(BoxType.EMPTY);

        // Shuffle
        Collections.shuffle(types);

        // Create boxes
        List<Box> boxes = new ArrayList<>();
        for (int i = 0; i < totalBoxes; i++) {
            int row = i / gridSize;
            int col = i % gridSize;

            Box box = Box.builder()
                    .boxId(UUID.randomUUID().toString())
                    .position(i)
                    .row(row)
                    .col(col)
                    .type(types.get(i))
                    .isOpened(false)
                    .heatmapValue(0)
                    .hoverCount(0)
                    .hoveredBy(new ArrayList<>())
                    .build();

            boxes.add(box);
        }

        return boxes;
    }

    public Box createTrapBox(int position, int row, int col) {
        return Box.builder()
                .boxId(UUID.randomUUID().toString())
                .position(position)
                .row(row)
                .col(col)
                .type(BoxType.TRAP)
                .isOpened(false)
                .value(100) // Default trap penalty
                .build();
    }

    public Box createJackpotBox(int position, int row, int col, int value) {
        return Box.builder()
                .boxId(UUID.randomUUID().toString())
                .position(position)
                .row(row)
                .col(col)
                .type(BoxType.JACKPOT)
                .isOpened(false)
                .value(value)
                .build();
    }
}
