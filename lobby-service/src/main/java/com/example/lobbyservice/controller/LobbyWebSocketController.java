package com.example.lobbyservice.controller;

import com.example.lobbyservice.dto.JoinRoomRequest;
import com.example.lobbyservice.dto.LobbyUpdateEvent;
import com.example.lobbyservice.service.LobbyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class LobbyWebSocketController {
    private final LobbyService lobbyService;
    @MessageMapping("/lobby.join/{roomCode}")
    public void joinLobby(@DestinationVariable String roomCode,
                          @Payload JoinRoomRequest request,
                          SimpMessageHeaderAccessor headerAccessor){
        log.info("WebSocket: Player {} joining lobby {}", request.getUsername(), roomCode);
        request.setConnectionId(headerAccessor.getSessionId());
        lobbyService.joinRoom(request);
        headerAccessor.getSessionAttributes().put("roomcode",roomCode);
        headerAccessor.getSessionAttributes().put("playerId",request.getPlayerId().toString());
    }
    @MessageMapping("/lobby.leave/{roomCode}")
    public void leaveLobby(@DestinationVariable String roomCode,
                           Principal principal,
                           SimpMessageHeaderAccessor headerAccessor){
        String playerId = (String) headerAccessor.getSessionAttributes().get("playerId");
        if (playerId!=null){
            log.info("WebSocket: Player {} leaving lobby {}", playerId, roomCode);
            lobbyService.leaveRoom(roomCode, UUID.fromString(playerId));
        }

    }
    @MessageMapping("/lobby.ready/{roomCode}")
    public void setReady(@DestinationVariable String roomCode,
                         @Payload boolean ready,
                         SimpMessageHeaderAccessor headerAccessor){
        String playerId = (String) headerAccessor.getSessionAttributes().get("playerId");
        if(playerId!=null){
            log.info("WebSocket: Player {} ready status: {} in lobby {}", playerId, ready, roomCode);
            lobbyService.setPlayerReady(roomCode,UUID.fromString(playerId),ready);
        }
    }
    @SubscribeMapping("/topic/lobby/{roomCode}")
    public LobbyUpdateEvent subscribeToLobby(@DestinationVariable String roomCode){
        log.info("Client subscribed to lobby: {}", roomCode);
        return LobbyUpdateEvent.builder()
                .eventType("SUBSCRIBED")
                .roomCode(roomCode)
                .build();
    }
}
