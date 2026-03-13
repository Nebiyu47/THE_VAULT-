package com.example.gameservice.controller;

import com.example.gameservice.dto.*;
import com.example.gameservice.model.GameResult;
import com.example.gameservice.service.GameService;
import com.example.gameservice.service.HeatmapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
@Slf4j
public class GameController {

    private final GameService gameService;
    private final HeatmapService heatmapService;

    @PostMapping("/start")
    public ResponseEntity<GameStateResponse> startGame(@Valid @RequestBody StartGameRequest request) {
        log.info("POST /api/game/start - Starting game for room: {}", request.getRoomCode());
        GameStateResponse response = gameService.startGame(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{gameId}/box/open")
    public ResponseEntity<BoxResultResponse> openBox(
            @PathVariable UUID gameId,
            @Valid @RequestBody OpenBoxRequest request) {
        request.setGameId(gameId);
        log.info("POST /api/game/{}/box/open - Player {} opening box", gameId, request.getPlayerId());
        BoxResultResponse response = gameService.openBox(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{gameId}/hover")
    public ResponseEntity<Void> updateHover(
            @PathVariable UUID gameId,
            @Valid @RequestBody HoverRequest request) {
        log.info("POST /api/game/{}/hover - Player {} hovering box {}",
                gameId, request.getPlayerId(), request.getBoxPosition());
        gameService.updateHover(gameId, request.getPlayerId(),
                request.getBoxPosition(), request.getAction());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{gameId}/state")
    public ResponseEntity<GameStateResponse> getGameState(
            @PathVariable UUID gameId,
            @RequestParam UUID playerId) {
        log.info("GET /api/game/{}/state - Getting state for player {}", gameId, playerId);
        GameStateResponse response = gameService.getGameState(gameId, playerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{gameId}/heatmap")
    public ResponseEntity<HeatmapResponse> getHeatmap(@PathVariable UUID gameId) {
        log.info("GET /api/game/{}/heatmap - Getting heatmap", gameId);
        HeatmapResponse response = heatmapService.getHeatmapData(gameId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{gameId}/end")
    public ResponseEntity<GameResult> endGame(
            @PathVariable UUID gameId,
            @RequestParam(required = false) UUID winnerId) {
        log.info("POST /api/game/{}/end - Ending game", gameId);
        GameResult result = gameService.endGame(gameId, winnerId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/history/{playerId}")
    public ResponseEntity<List<GameResult>> getPlayerHistory(@PathVariable UUID playerId) {
        log.info("GET /api/game/history/{} - Getting player history", playerId);
        // Implement player history retrieval
        return ResponseEntity.ok(null);
    }
}