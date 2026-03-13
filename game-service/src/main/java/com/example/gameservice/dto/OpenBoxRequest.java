package com.example.gameservice.dto;

import jakarta.validation.constraints.Min;
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
public class OpenBoxRequest {

   @NotNull(message = "Game id is Required")
    private UUID gameId;
   @NotNull(message = "Player Id is required")
   private UUID playerId;
   @Min(value =0 , message = "Box postions must be between 0 and 24")
    private int boxPosition;
   private String boxId;

}
