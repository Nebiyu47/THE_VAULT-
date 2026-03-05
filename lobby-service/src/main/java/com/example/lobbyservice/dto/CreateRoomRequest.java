package com.example.lobbyservice.dto;

import com.example.lobbyservice.model.GameConfig;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoomRequest {

    @NotBlank(message = "Room name is required")
    @Size(min = 3, max = 50, message = "Room name must be between 3 and 50 characters")
    private String roomName;

    @Min(value = 2, message = "Minimum players must be at least 2")
    @Max(value = 8, message = "Maximum players cannot exceed 8")
    private int maxPlayers = 8;

    private boolean isPrivate = false;

    private String password;

    private GameConfig gameConfig;

    private UUID architectId;

    private String architectName;
}
