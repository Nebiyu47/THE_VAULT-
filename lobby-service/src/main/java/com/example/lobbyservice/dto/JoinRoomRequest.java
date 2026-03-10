package com.example.lobbyservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JoinRoomRequest {
    @NotBlank(message = "Room code is required")
    @Size(min=6 , max=6 ,message = "Room code must be 6 characters" )
    private String roomCode;
    private String password;
    private UUID playerId;
    private String username;
    private String avatarUrl;
    private String connectionId;
}
