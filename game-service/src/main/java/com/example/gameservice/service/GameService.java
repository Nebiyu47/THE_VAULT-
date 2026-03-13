package com.example.gameservice.service;

import com.example.gameservice.dto.*;
import com.example.gameservice.model.*;
import com.example.gameservice.model.enums.BoxType;
import com.example.gameservice.model.enums.Difficulty;
import com.example.gameservice.model.enums.EventType;
import com.example.gameservice.model.enums.GameStatus;
import com.example.gameservice.repository.GameRepository;
import com.example.gameservice.repository.MoveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;
    private final BoxService boxService;
    private final HeatmapService heatmapService;
    private final GreedTimerService greedTimerService;
    private final GameResultService gameResultService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    private final Map<UUID, ScheduledFuture<?>> gameTimers = new ConcurrentHashMap<>();
    private final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(10);

    private static final String GAME_CACHE_PREFIX = "game:";
    private static final String PLAYER_GAME_PREFIX = "player:game:";

    @Transactional
    public GameStateResponse startGame(StartGameRequest request) {
        log.info("Starting game for room: {}", request.getRoomCode());

        // Generate game ID
        UUID gameId = UUID.randomUUID();

        // Create game instance
        Game game = Game.builder()
                .gameId(gameId)
                .roomCode(request.getRoomCode())
                .status(GameStatus.ACTIVE)
                .startTime(LocalDateTime.now())
                .architectId(request.getArchitectId())
                .config(request.getConfig() != null ? request.getConfig() : new GameConfig())
                .jackpotAmount(request.getCustomJackpot() > 0 ?
                        request.getCustomJackpot() : request.getConfig().getBaseJackpot())
                .currentJackpot(0)
                .greedTimerSeconds(request.getConfig().getGreedTimerSeconds())
                .isGreedTimerActive(false)
                .lastActivity(LocalDateTime.now())
                .build();

        // Initialize boxes
        List<Box> boxes = boxService.initializeBoxes(
                game.getConfig().getGridSize(),
                game.getConfig().getJackpotCount(),
                game.getConfig().getTrapCount(),
                game.getConfig().getDifficulty()
        );
        game.setBoxes(boxes);
        game.setTotalBoxes(boxes.size());

        // Save game
        game = gameRepository.save(game);

        // Cache in Redis
        cacheGame(game);

        // Track players in game
        game.getPlayers().forEach(player -> {
            String playerGameKey = PLAYER_GAME_PREFIX + player.getPlayerId();
            redisTemplate.opsForValue().set(playerGameKey, gameId.toString(), 1, TimeUnit.HOURS);
        });

        // Broadcast game start
        GameEvent event = GameEvent.builder()
                .eventType(EventType.GAME_STARTED.name())
                .gameId(gameId)
                .roomCode(request.getRoomCode())
                .data(game)
                .triggeredBy(request.getArchitectId())
                .timestamp(LocalDateTime.now())
                .build();

        broadcastGameEvent(gameId, event);

        // Send to message queue for analytics
        rabbitTemplate.convertAndSend("game.exchange", "game.started", event);

        log.info("Game started successfully with ID: {}", gameId);

        return GameStateResponse.fromGame(game, true);
    }

    @Transactional
    public BoxResultResponse openBox(OpenBoxRequest request) {
        log.info("Opening box at position {} in game {}", request.getBoxPosition(), request.getGameId());

        Game game = getGame(request.getGameId());

        // Validate game state
        validateGameForMove(game, request);

        // Find box
        Box box = game.getBoxes().stream()
                .filter(b -> b.getPosition() == request.getBoxPosition())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Box not found at position: " + request.getBoxPosition()));

        // Check if already opened
        if (box.isOpened()) {
            throw new RuntimeException("Box already opened");
        }

        // Process box opening
        BoxResultResponse result = processBoxOpening(game, box, request.getPlayerId());

        // Create move record
        Move move = createMove(game, box, request.getPlayerId(), result);
        game.getMoves().add(move);
        moveRepository.save(move);

        // Update player stats
        updatePlayerStats(game, request.getPlayerId(), box);

        // Update game state
        game.setOpenedBoxes(game.getOpenedBoxes() + 1);
        game.setLastActivity(LocalDateTime.now());

        // Check if game should end
        if (shouldEndGame(game, result)) {
            endGame(game.getGameId(), result.getWinnerId());
            result.setGameEnded(true);
        } else {
            // Start greed timer if this was a jackpot
            if (box.getType() == BoxType.JACKPOT && !game.isGreedTimerActive()) {
                startGreedTimer(game);
            }

            gameRepository.save(game);
        }

        // Update cache
        cacheGame(game);

        // Broadcast box opening
        broadcastBoxOpened(game, result);

        // Update heatmap
        heatmapService.updateHeatmap(game.getGameId(), request.getBoxPosition(), request.getPlayerId(), "open");

        log.info("Box opened successfully. Type: {}, Value: {}", box.getType(), result.getValue());

        return result;
    }

    @Transactional
    public void updateHover(UUID gameId, UUID playerId, int boxPosition, String action) {
        Game game = getGame(gameId);

        if (game.getStatus() != GameStatus.ACTIVE) {
            return; // Ignore hovers for inactive games
        }

        // Update heatmap
        heatmapService.updateHeatmap(gameId, boxPosition, playerId, action);

        // Update player's current hover
        game.getPlayers().stream()
                .filter(p -> p.getPlayerId().equals(playerId))
                .findFirst()
                .ifPresent(player -> {
                    if ("enter".equals(action)) {
                        player.setCurrentHoverPosition(String.valueOf(boxPosition));
                    } else {
                        player.setCurrentHoverPosition(null);
                    }
                });

        // Cache update
        cacheGame(game);

        // Broadcast heatmap update periodically (throttled in HeatmapService)
    }

    @Transactional
    public GameResult endGame(UUID gameId, UUID winnerId) {
        log.info("Ending game: {} with winner: {}", gameId, winnerId);

        Game game = getGame(gameId);

        if (game.getStatus() == GameStatus.COMPLETED) {
            return gameResultService.getGameResult(gameId);
        }

        game.setStatus(GameStatus.COMPLETED);
        game.setEndTime(LocalDateTime.now());
        game.setWinnerId(winnerId);

        // Find winner name
        game.getPlayers().stream()
                .filter(p -> p.getPlayerId().equals(winnerId))
                .findFirst()
                .ifPresent(winner -> game.setWinnerName(winner.getUsername()));

        // Calculate results
        GameResult result = gameResultService.calculateGameResult(game);

        // Stop any timers
        cancelGameTimer(gameId);

        // Save game and result
        gameRepository.save(game);
        gameResultService.saveGameResult(result);

        // Clear cache
        clearGameCache(gameId);

        // Broadcast game end
        GameEvent event = GameEvent.builder()
                .eventType(EventType.GAME_ENDED.name())
                .gameId(gameId)
                .roomCode(game.getRoomCode())
                .data(result)
                .timestamp(LocalDateTime.now())
                .build();

        broadcastGameEvent(gameId, event);

        // Send to message queue for leaderboard updates
        rabbitTemplate.convertAndSend("game.exchange", "game.ended", result);

        log.info("Game ended successfully. Winner: {}", winnerId);

        return result;
    }

    public GameStateResponse getGameState(UUID gameId, UUID playerId) {
        Game game = getGame(gameId);

        boolean isArchitect = game.getArchitectId().equals(playerId);

        GameStateResponse response = GameStateResponse.fromGame(game, isArchitect);

        // Calculate remaining greed timer seconds
        if (game.isGreedTimerActive() && game.getGreedTimerStarted() != null) {
            long elapsedSeconds = ChronoUnit.SECONDS.between(game.getGreedTimerStarted(), LocalDateTime.now());
            int remaining = Math.max(0, game.getGreedTimerSeconds() - (int) elapsedSeconds);
            response.setRemainingGreedSeconds(remaining);
        }

        // Add recent moves
        List<MoveResponse> recentMoves = game.getMoves().stream()
                .sorted((m1, m2) -> m2.getTimestamp().compareTo(m1.getTimestamp()))
                .limit(10)
                .map(this::convertToMoveResponse)
                .toList();
        response.setRecentMoves(recentMoves);

        return response;
    }

    public HeatmapResponse getHeatmap(UUID gameId) {
        return heatmapService.getHeatmapData(gameId);
    }

    private BoxResultResponse processBoxOpening(Game game, Box box, UUID playerId) {
        BoxResultResponse result = BoxResultResponse.builder()
                .boxId(box.getBoxId())
                .position(box.getPosition())
                .row(box.getRow())
                .col(box.getCol())
                .type(box.getType())
                .isOpened(true)
                .openedBy(playerId)
                .value(0)
                .build();

        // Mark box as opened
        box.setOpened(true);
        box.setOpenedBy(playerId);
        box.setOpenedAt(LocalDateTime.now());

        switch (box.getType()) {
            case JACKPOT:
                int jackpotValue = game.getCurrentJackpot() + game.getJackpotAmount();
                box.setValue(jackpotValue);
                result.setValue(jackpotValue);
                result.setMessage("JACKPOT! You won " + jackpotValue + " points!");

                // Update jackpot
                game.setCurrentJackpot(0);
                result.setNewJackpot(0);

                // Check if this player wins
                if (game.getPlayers().size() == 1) {
                    result.setWinnerId(playerId);
                }
                break;

            case TRAP:
                int trapPenalty = calculateTrapPenalty(game.getConfig().getDifficulty());
                box.setValue(-trapPenalty);
                result.setValue(-trapPenalty);
                result.setMessage("TRAP! You lost " + trapPenalty + " points!");

                // Add to jackpot
                game.setCurrentJackpot(game.getCurrentJackpot() + trapPenalty);
                result.setNewJackpot(game.getCurrentJackpot());
                break;

            case EMPTY:
                result.setMessage("Empty box. Nothing happened.");
                result.setNewJackpot(game.getCurrentJackpot());
                break;
        }

        return result;
    }

    private Move createMove(Game game, Box box, UUID playerId, BoxResultResponse result) {
        GamePlayer player = game.getPlayers().stream()
                .filter(p -> p.getPlayerId().equals(playerId))
                .findFirst()
                .orElse(null);

        return Move.builder()
                .moveId(UUID.randomUUID())
                .gameId(game.getGameId())
                .playerId(playerId)
                .playerName(player != null ? player.getUsername() : "Unknown")
                .boxId(box.getBoxId())
                .position(box.getPosition())
                .boxType(box.getType())
                .timestamp(LocalDateTime.now())
                .pointsEarned(result.getValue())
                .isWinningMove(playerId.equals(result.getWinnerId()))
                .jackpotAmount(result.getNewJackpot())
                .metadata(Map.of(
                        "remainingBoxes", game.getTotalBoxes() - game.getOpenedBoxes() - 1,
                        "currentJackpot", result.getNewJackpot()
                ))
                .build();
    }

    private void startGreedTimer(Game game) {
        game.setGreedTimerActive(true);
        game.setGreedTimerStarted(LocalDateTime.now());

        // Schedule timer expiration
        ScheduledFuture<?> future = scheduler.schedule(
                () -> handleGreedTimerExpiration(game.getGameId()),
                game.getGreedTimerSeconds(),
                TimeUnit.SECONDS
        );

        gameTimers.put(game.getGameId(), future);

        // Broadcast timer start
        GameEvent event = GameEvent.builder()
                .eventType(EventType.GREED_TIMER_STARTED.name())
                .gameId(game.getGameId())
                .roomCode(game.getRoomCode())
                .data(Map.of("seconds", game.getGreedTimerSeconds()))
                .timestamp(LocalDateTime.now())
                .build();

        broadcastGameEvent(game.getGameId(), event);

        log.info("Greed timer started for game: {}", game.getGameId());
    }

    private void handleGreedTimerExpiration(UUID gameId) {
        try {
            Game game = getGame(gameId);

            if (game.getStatus() == GameStatus.ACTIVE && game.isGreedTimerActive()) {
                log.info("Greed timer expired for game: {}", gameId);

                // Find player with least points or random elimination
                UUID eliminatedPlayer = selectPlayerForElimination(game);

                // Eliminate player
                eliminatePlayer(game, eliminatedPlayer);

                // Reset timer
                game.setGreedTimerActive(false);
                game.setGreedTimerStarted(null);

                // Check if game should end
                if (game.getPlayers().stream().filter(GamePlayer::isActive).count() <= 1) {
                    UUID winner = game.getPlayers().stream()
                            .filter(GamePlayer::isActive)
                            .findFirst()
                            .map(GamePlayer::getPlayerId)
                            .orElse(null);
                    endGame(gameId, winner);
                } else {
                    gameRepository.save(game);
                    cacheGame(game);
                }

                // Broadcast timer expiration
                GameEvent event = GameEvent.builder()
                        .eventType(EventType.GREED_TIMER_EXPIRED.name())
                        .gameId(gameId)
                        .roomCode(game.getRoomCode())
                        .data(Map.of("eliminatedPlayer", eliminatedPlayer))
                        .timestamp(LocalDateTime.now())
                        .build();

                broadcastGameEvent(gameId, event);
            }
        } catch (Exception e) {
            log.error("Error handling greed timer expiration: {}", e.getMessage());
        }
    }

    private UUID selectPlayerForElimination(Game game) {
        // Get active players
        List<GamePlayer> activePlayers = game.getPlayers().stream()
                .filter(GamePlayer::isActive)
                .toList();

        if (activePlayers.isEmpty()) {
            return null;
        }

        // Find player with least points
        return activePlayers.stream()
                .min(Comparator.comparingInt(GamePlayer::getPointsEarned))
                .map(GamePlayer::getPlayerId)
                .orElse(activePlayers.get(0).getPlayerId());
    }

    private void eliminatePlayer(Game game, UUID playerId) {
        game.getPlayers().stream()
                .filter(p -> p.getPlayerId().equals(playerId))
                .findFirst()
                .ifPresent(player -> {
                    player.setActive(false);
                    log.info("Player {} eliminated from game {}", playerId, game.getGameId());
                });
    }

    private void validateGameForMove(Game game, OpenBoxRequest request) {
        if (game.getStatus() != GameStatus.ACTIVE) {
            throw new RuntimeException("Game is not active");
        }

        // Check if player is in game and active
        boolean playerInGame = game.getPlayers().stream()
                .anyMatch(p -> p.getPlayerId().equals(request.getPlayerId()) && p.isActive());

        if (!playerInGame) {
            throw new RuntimeException("Player not in game or has been eliminated");
        }

        // Check if greed timer is active
        if (game.isGreedTimerActive()) {
            long elapsedSeconds = ChronoUnit.SECONDS.between(game.getGreedTimerStarted(), LocalDateTime.now());
            if (elapsedSeconds > game.getGreedTimerSeconds()) {
                game.setGreedTimerActive(false);
            }
        }
    }

    private boolean shouldEndGame(Game game, BoxResultResponse result) {
        // Check if all boxes opened
        if (game.getOpenedBoxes() >= game.getTotalBoxes()) {
            return true;
        }

        // Check if jackpot was found and only one player remains
        if (result.getType() == BoxType.JACKPOT) {
            long activePlayers = game.getPlayers().stream()
                    .filter(GamePlayer::isActive)
                    .count();
            if (activePlayers == 1) {
                result.setWinnerId(game.getPlayers().stream()
                        .filter(GamePlayer::isActive)
                        .findFirst()
                        .map(GamePlayer::getPlayerId)
                        .orElse(null));
                return true;
            }
        }

        return false;
    }

    private void updatePlayerStats(Game game, UUID playerId, Box box) {
        game.getPlayers().stream()
                .filter(p -> p.getPlayerId().equals(playerId))
                .findFirst()
                .ifPresent(player -> {
                    player.setBoxesOpened(player.getBoxesOpened() + 1);
                    player.setLastMoveAt(LocalDateTime.now());

                    if (box.getType() == BoxType.JACKPOT) {
                        player.setPointsEarned(player.getPointsEarned() + box.getValue());
                    } else if (box.getType() == BoxType.TRAP) {
                        player.setPointsEarned(player.getPointsEarned() - box.getValue());
                    }
                });
    }

    private int calculateTrapPenalty(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 50;
            case MEDIUM -> 100;
            case HARD -> 200;
            case EXPERT -> 500;
        };
    }

    public Game getGame(UUID gameId) {
        // Try cache first
        String cacheKey = GAME_CACHE_PREFIX + gameId;
        Game game = (Game) redisTemplate.opsForValue().get(cacheKey);

        if (game == null) {
            // Try database
            game = gameRepository.findByGameId(gameId)
                    .orElseThrow(() -> new RuntimeException("Game not found: " + gameId));

            // Cache it
            cacheGame(game);
        }

        return game;
    }

    private void cacheGame(Game game) {
        String cacheKey = GAME_CACHE_PREFIX + game.getGameId();
        redisTemplate.opsForValue().set(cacheKey, game, 1, TimeUnit.HOURS);
    }

    private void clearGameCache(UUID gameId) {
        String cacheKey = GAME_CACHE_PREFIX + gameId;
        redisTemplate.delete(cacheKey);
    }

    private void cancelGameTimer(UUID gameId) {
        ScheduledFuture<?> future = gameTimers.remove(gameId);
        if (future != null) {
            future.cancel(false);
        }
    }

    private void broadcastGameEvent(UUID gameId, GameEvent event) {
        messagingTemplate.convertAndSend("/topic/game/" + gameId, event);
    }

    private void broadcastBoxOpened(Game game, BoxResultResponse result) {
        GameEvent event = GameEvent.builder()
                .eventType(EventType.BOX_OPENED.name())
                .gameId(game.getGameId())
                .roomCode(game.getRoomCode())
                .data(result)
                .timestamp(LocalDateTime.now())
                .build();

        broadcastGameEvent(game.getGameId(), event);
    }

    private MoveResponse convertToMoveResponse(Move move) {
        return MoveResponse.builder()
                .playerId(move.getPlayerId())
                .playerName(move.getPlayerName())
                .position(move.getPosition())
                .type(move.getBoxType())
                .timestamp(move.getTimestamp())
                .pointsEarned(move.getPointsEarned())
                .isWinningMove(move.isWinningMove())
                .build();
    }
}