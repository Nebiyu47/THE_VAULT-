package com.example.gameservice.controller;

import com.example.gameservice.dto.HoverRequest;
import com.example.gameservice.dto.OpenBoxRequest;
import com.example.gameservice.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class GameWebSocketController {

    private final GameService gameService;

    @MessageMapping("/game.{gameId}.open")
    public void openBox(@DestinationVariable UUID gameId, @Payload OpenBoxRequest request) {
        request.setGameId(gameId);
        log.info("WebSocket: Player {} opening box in game {}", request.getPlayerId(), gameId);
        gameService.openBox(request);
    }

    @MessageMapping("/game.{gameId}.hover")
    public void updateHover(@DestinationVariable UUID gameId, @Payload HoverRequest request) {
        request.setGameId(gameId);
        log.debug("WebSocket: Player {} hovering box {} in game {}",
                request.getPlayerId(), request.getBoxPosition(), gameId);
        gameService.updateHover(gameId, request.getPlayerId(),
                request.getBoxPosition(), request.getAction());
    }
}