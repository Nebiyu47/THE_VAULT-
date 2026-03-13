package com.example.gameservice.dto;

import com.example.gameservice.model.GameConfig;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class StartGameRequest {
   @NotBlank(message = "Room code is required")
   private String roomCode;
   @NotNull(message = "Architect Id is required")
   private UUID architectId;
   private GameConfig config;
   @Min(value = 1 , message = "Jackpot amount must be at least 1")
   private int customJackpot;

}
