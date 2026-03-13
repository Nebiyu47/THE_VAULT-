package com.example.gameservice.service;

import com.example.gameservice.dto.GameEvent;
import com.example.gameservice.model.Game;
import com.example.gameservice.model.enums.EventType;
import com.example.gameservice.model.enums.GameStatus;
import com.example.gameservice.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


@Service

@Slf4j
public class GreedTimerService {

    private final GameRepository gameRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final GameService gameService;

    private final Map<UUID, TimerInfo> activeTimers = new ConcurrentHashMap<>();
    // Remove @RequiredArgsConstructor and write the constructor manually
    public GreedTimerService(GameRepository gameRepository,
                             SimpMessagingTemplate messagingTemplate,
                             @Lazy GameService gameService) { // <--- @Lazy is the magic fix
        this.gameRepository = gameRepository;
        this.messagingTemplate = messagingTemplate;
        this.gameService = gameService;
    }
    @Scheduled(fixedRate = 1000) // Every second
    public void processGreedTimers() {
        LocalDateTime now = LocalDateTime.now();

        activeTimers.forEach((gameId, timerInfo) -> {
            try {
                Game game = gameService.getGame(gameId);

                if (game.getStatus() != GameStatus.ACTIVE || !game.isGreedTimerActive()) {
                    activeTimers.remove(gameId);
                    return;
                }

                long elapsedSeconds = ChronoUnit.SECONDS.between(game.getGreedTimerStarted(), now);
                int remaining = game.getGreedTimerSeconds() - (int) elapsedSeconds;

                if (remaining <= 0) {
                    // Timer expired - handled by GameService
                    activeTimers.remove(gameId);
                } else {
                    // Broadcast remaining time
                    if (remaining <= 10 || remaining % 5 == 0) { // More frequent updates for last 10 seconds
                        broadcastTimerUpdate(gameId, remaining);
                    }
                }
            } catch (Exception e) {
                log.error("Error processing greed timer for game {}: {}", gameId, e.getMessage());
                activeTimers.remove(gameId);
            }
        });
    }

    public void startTimer(UUID gameId, int seconds) {
        activeTimers.put(gameId, new TimerInfo(seconds, System.currentTimeMillis()));
        log.info("Started greed timer for game {}: {} seconds", gameId, seconds);
    }

    public void stopTimer(UUID gameId) {
        activeTimers.remove(gameId);
        log.info("Stopped greed timer for game {}", gameId);
    }

    private void broadcastTimerUpdate(UUID gameId, int remainingSeconds) {
        GameEvent event = GameEvent.builder()
                .eventType(EventType.GREED_TIMER_TICK.name())
                .gameId(gameId)
                .data(Map.of("remaining", remainingSeconds))
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/game/" + gameId, event);
    }

    private static class TimerInfo {
        final int totalSeconds;
        final long startTime;

        TimerInfo(int totalSeconds, long startTime) {
            this.totalSeconds = totalSeconds;
            this.startTime = startTime;
        }
    }
}