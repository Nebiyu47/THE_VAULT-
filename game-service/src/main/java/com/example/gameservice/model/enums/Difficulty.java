package com.example.gameservice.model.enums;

public enum Difficulty {
    EASY(1.0),    // Lower trap probability
    MEDIUM(1.5),  // Normal
    HARD(2.0),    // Higher trap probability
    EXPERT(2.5);  // Very high trap probability

    public final double trapMultiplier;

    Difficulty(double trapMultiplier) {
        this.trapMultiplier = trapMultiplier;
    }
}