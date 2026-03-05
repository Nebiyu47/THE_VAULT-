package com.example.lobbyservice.service;

import com.example.lobbyservice.enums.PlayerRole;
import com.example.lobbyservice.enums.RoomStatus;
import com.example.lobbyservice.model.GameRoom;
import com.example.lobbyservice.model.LobbyPlayer;
import com.example.lobbyservice.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchmakingService {
    private final RoomRepository roomRepository;
    private final LobbyService lobbyService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    @Async
    public void scheduleGameStart(String roomCode, int countdownSeconds) {
        scheduler.schedule(() -> {
            try {
                GameRoom room = roomRepository.findByRoomCode(roomCode).orElse(null);
                if (room != null && room.getStatus() == RoomStatus.COUNTDOWN) {
                    // Check if all players are still ready
                    boolean allReady = room.getPlayers().stream()
                            .allMatch(p -> p.isReady() || p.getRole() == PlayerRole.SPECTATOR);

                    if (allReady && room.getPlayers().size() >= room.getMinPlayers()) {
                        lobbyService.startGame(roomCode);
                    } else {
                        // Cancel countdown
                        room.setStatus(RoomStatus.WAITING);
                        roomRepository.save(room);
                        log.info("Countdown cancelled for room: {}", roomCode);
                    }
                }
            } catch (Exception e) {
                log.error("Error starting game: {}", e.getMessage());
            }
        }, countdownSeconds, TimeUnit.SECONDS);
    }
    public List<GameRoom> findAvailableRooms(int playerCount){
        return roomRepository.findByStatus(RoomStatus.WAITING).stream()
                .filter(room -> room.getPlayers().size()<room.getMaxPlayers())
                .filter(room -> room.getPlayers().size()+ playerCount<=room.getMaxPlayers())
                .toList();
    }
    public GameRoom findOptimalRoom(int playerCount , int averageSkill){
        return findAvailableRooms(playerCount).stream()
                .findFirst()
                .orElse(null);
    }
}
