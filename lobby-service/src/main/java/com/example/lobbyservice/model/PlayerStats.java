package com.example.lobbyservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerStats implements Serializable {

    private int totalGames;

    private int wins;

    private int losses;

    private int vaultPoints;

    private int boxesOpened;

    private int trapsTriggered;

    private int jackpotsFound;
}

