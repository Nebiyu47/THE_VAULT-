package com.example.lobbyservice.service;

import com.example.lobbyservice.dto.*;
import com.example.lobbyservice.enums.EventType;
import com.example.lobbyservice.enums.PlayerRole;
import com.example.lobbyservice.enums.RoomStatus;
import com.example.lobbyservice.model.GameConfig;
import com.example.lobbyservice.model.GameRoom;
import com.example.lobbyservice.model.LobbyPlayer;
import com.example.lobbyservice.model.LobbyState;

import com.example.lobbyservice.repository.mongodb.LobbyStateRepository;
import com.example.lobbyservice.repository.mongodb.RoomRepository;
import com.example.lobbyservice.repository.redis.PlayerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LobbyService {

    private final RoomRepository roomRepository;
    private final LobbyStateRepository lobbyStateRepository;
    private final PlayerRepository playerRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private  MatchmakingService matchmakingService;

    @Value("${lobby.room.code-length:6}")
    private int roomCodeLength;

    @Value("${lobby.room.max-players:8}")
    private int maxPlayers;

    @Value("${lobby.room.idle-timeout:300}")
    private int idleTimeout;
    @Autowired
    public void setMatchmakingService(@Lazy MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @Transactional
    public RoomResponse createRoom(CreateRoomRequest request) {
        log.info("Creating new room: {}", request.getRoomName());

        // Generate unique room code
        String roomCode = generateUniqueRoomCode();

        // Create default game config if not provided
        GameConfig config = request.getGameConfig();
        if (config == null) {
            config = GameConfig.builder().build();
        }

        // Create architect player
        LobbyPlayer architect = LobbyPlayer.builder()
                .playerId(request.getArchitectId())
                .username(request.getArchitectName())
                .role(PlayerRole.ARCHITECT)
                .isReady(true)
                .joinedAt(LocalDateTime.now())
                .isOnline(true)
                .build();

        // Create room
        GameRoom room = GameRoom.builder()
                .roomCode(roomCode)
                .name(request.getRoomName())
                .status(RoomStatus.WAITING)
                .architectId(request.getArchitectId())
                .architectName(request.getArchitectName())
                .players(List.of(architect))
                .maxPlayers(request.getMaxPlayers())
                .minPlayers(2)
                .isPrivate(request.isPrivate())
                .password(request.getPassword())
                .gameConfig(config)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        room = roomRepository.save(room);

        // Create lobby state in Redis
        LobbyState lobbyState = LobbyState.builder()
                .id(roomCode)
                .roomCode(roomCode)
                .status(RoomStatus.WAITING)
                .playerCount(1)
                .readyCount(1)
                .countdown(0)
                .allPlayersReady(false)
                .build();

        lobbyStateRepository.save(lobbyState);

        // Cache room in Redis
        String roomKey = "room:" + roomCode;
        redisTemplate.opsForValue().set(roomKey, room, idleTimeout, TimeUnit.SECONDS);

        // Track player in room
        String playerRoomKey = "player:room:" + request.getArchitectId();
        redisTemplate.opsForValue().set(playerRoomKey, roomCode, idleTimeout, TimeUnit.SECONDS);

        log.info("Room created successfully with code: {}", roomCode);

        return RoomResponse.fromGameRoom(room);
    }

    @Transactional
    public RoomResponse joinRoom(JoinRoomRequest request) {
        log.info("Player {} joining room: {}", request.getUsername(), request.getRoomCode());

        String roomCode = request.getRoomCode().toUpperCase();

        // Get room from cache or database
        GameRoom room = getRoomByCode(roomCode);

        // Validate room
        validateRoomForJoin(room, request);

        // Check if player already in room
        boolean alreadyInRoom = room.getPlayers().stream()
                .anyMatch(p -> p.getPlayerId().equals(request.getPlayerId()));

        LobbyPlayer player;
        if (alreadyInRoom) {
            // Update existing player
            player = room.getPlayers().stream()
                    .filter(p -> p.getPlayerId().equals(request.getPlayerId()))
                    .findFirst()
                    .get();
            player.setOnline(true);
            player.setConnectionId(request.getConnectionId());
        } else {
            // Check if room is full
            if (room.getPlayers().size() >= room.getMaxPlayers()) {
                throw new RuntimeException("Room is full");
            }

            // Create new player
            player = LobbyPlayer.builder()
                    .playerId(request.getPlayerId())
                    .username(request.getUsername())
                    .avatarUrl(request.getAvatarUrl())
                    .role(PlayerRole.BREACHER)
                    .isReady(false)
                    .joinedAt(LocalDateTime.now())
                    .isOnline(true)
                    .connectionId(request.getConnectionId())
                    .build();

            room.getPlayers().add(player);
        }

        room.setUpdatedAt(LocalDateTime.now());
        room = roomRepository.save(room);

        // Update Redis cache
        updateRoomCache(room);

        // Update lobby state
        updateLobbyState(room);

        // Track player in room
        String playerRoomKey = "player:room:" + request.getPlayerId();
        redisTemplate.opsForValue().set(playerRoomKey, roomCode, idleTimeout, TimeUnit.SECONDS);

        // Broadcast update to all players in room
        broadcastLobbyUpdate(room, EventType.PLAYER_JOINED, request.getPlayerId());

        log.info("Player {} joined room {} successfully", request.getUsername(), roomCode);

        return RoomResponse.fromGameRoom(room);
    }

    @Transactional
    public void leaveRoom(String roomCode, UUID playerId) {
        log.info("Player {} leaving room: {}", playerId, roomCode);

        GameRoom room = getRoomByCode(roomCode);

        // Remove player from room
        boolean removed = room.getPlayers().removeIf(p -> p.getPlayerId().equals(playerId));

        if (!removed) {
            log.warn("Player {} not found in room {}", playerId, roomCode);
            return;
        }

        // If room becomes empty, delete it
        if (room.getPlayers().isEmpty()) {
            deleteRoom(roomCode);
            return;
        }

        // If architect leaves, assign new architect
        if (room.getArchitectId().equals(playerId) && !room.getPlayers().isEmpty()) {
            LobbyPlayer newArchitect = room.getPlayers().get(0);
            newArchitect.setRole(PlayerRole.ARCHITECT);
            room.setArchitectId(newArchitect.getPlayerId());
            room.setArchitectName(newArchitect.getUsername());
        }

        room.setUpdatedAt(LocalDateTime.now());
        room = roomRepository.save(room);

        // Update Redis cache
        updateRoomCache(room);

        // Update lobby state
        updateLobbyState(room);

        // Remove player tracking
        String playerRoomKey = "player:room:" + playerId;
        redisTemplate.delete(playerRoomKey);

        // Broadcast update
        broadcastLobbyUpdate(room, EventType.PLAYER_LEFT, playerId);

        log.info("Player {} left room {}", playerId, roomCode);
    }

    @Transactional
    public void setPlayerReady(String roomCode, UUID playerId, boolean ready) {
        log.info("Setting player {} ready status to {} in room {}", playerId, ready, roomCode);

        GameRoom room = getRoomByCode(roomCode);

        // Find and update player
        room.getPlayers().stream()
                .filter(p -> p.getPlayerId().equals(playerId))
                .findFirst()
                .ifPresent(player -> player.setReady(ready));

        // Update room in database
        roomRepository.updatePlayerReadyStatus(roomCode, playerId, ready);

        // Update Redis cache
        updateRoomCache(room);

        // Update lobby state
        updateLobbyState(room);

        // Check if all players ready
        boolean allReady = room.getPlayers().stream()
                .filter(p -> p.getRole() != PlayerRole.SPECTATOR)
                .allMatch(LobbyPlayer::isReady);

        if (allReady && room.getPlayers().size() >= room.getMinPlayers()) {
            // Start countdown
            startCountdown(roomCode);
        }

        // Broadcast update
        EventType eventType = ready ?
                EventType.PLAYER_READY :
                EventType.PLAYER_UNREADY;
        broadcastLobbyUpdate(room, eventType, playerId);
    }

    @Transactional
    public void startCountdown(String roomCode) {
        log.info("Starting countdown for room: {}", roomCode);

        GameRoom room = getRoomByCode(roomCode);
        room.setStatus(RoomStatus.COUNTDOWN);
        roomRepository.save(room);

        // Update Redis
        updateRoomCache(room);

        // Broadcast countdown start
        LobbyUpdateEvent event = LobbyUpdateEvent.builder()
                .eventType(EventType.GAME_STARTING.name())
                .roomCode(roomCode)
                .status(RoomStatus.COUNTDOWN)
                .countdown(5)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/lobby/" + roomCode, event);

        // Schedule game start
        matchmakingService.scheduleGameStart(roomCode, 5);
    }

    @Transactional
    public void startGame(String roomCode) {
        log.info("Starting game for room: {}", roomCode);

        GameRoom room = getRoomByCode(roomCode);
        room.setStatus(RoomStatus.IN_GAME);
        room.setStartedAt(LocalDateTime.now());
        roomRepository.save(room);

        // Update Redis
        updateRoomCache(room);

        // Broadcast game start
        LobbyUpdateEvent event = LobbyUpdateEvent.builder()
                .eventType(EventType.GAME_STARTED.name())
                .roomCode(roomCode)
                .status(RoomStatus.IN_GAME)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/lobby/" + roomCode, event);
    }

    public RoomResponse getRoom(String roomCode) {
        GameRoom room = getRoomByCode(roomCode);
        return RoomResponse.fromGameRoom(room);
    }

    public List<PlayerResponse> getRoomPlayers(String roomCode) {
        GameRoom room = getRoomByCode(roomCode);
        return room.getPlayers().stream()
                .map(PlayerResponse::fromLobbyPlayer)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> searchRooms(String query) {
        // Search by room code or name
        return roomRepository.findAll().stream()
                .filter(room -> room.getStatus() ==RoomStatus.WAITING)
                .filter(room -> room.getRoomCode().contains(query.toUpperCase()) ||
                        room.getName().toLowerCase().contains(query.toLowerCase()))
                .map(RoomResponse::fromGameRoom)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateGameConfig(String roomCode, GameConfig config, UUID architectId) {
        GameRoom room = getRoomByCode(roomCode);

        // Verify architect
        if (!room.getArchitectId().equals(architectId)) {
            throw new RuntimeException("Only architect can update game configuration");
        }

        room.setGameConfig(config);
        room.setUpdatedAt(LocalDateTime.now());
        roomRepository.save(room);

        // Update cache
        updateRoomCache(room);

        // Broadcast update
        broadcastLobbyUpdate(room, EventType.CONFIG_UPDATED, architectId);
    }

    @Transactional
    public void deleteRoom(String roomCode) {
        log.info("Deleting room: {}", roomCode);

        GameRoom room = getRoomByCode(roomCode);

        // Notify all players
        LobbyUpdateEvent event = LobbyUpdateEvent.builder()
                .eventType(EventType.LOBBY_CLOSED.name())
                .roomCode(roomCode)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/lobby/" + roomCode, event);

        // Remove from database
        roomRepository.deleteByRoomCode(roomCode);

        // Remove from Redis
        String roomKey = "room:" + roomCode;
        redisTemplate.delete(roomKey);

        // Remove lobby state
        lobbyStateRepository.deleteByRoomCode(roomCode);

        // Remove player tracking
        room.getPlayers().forEach(player -> {
            String playerRoomKey = "player:room:" + player.getPlayerId();
            redisTemplate.delete(playerRoomKey);
        });
    }

    @Scheduled(fixedDelayString = "${lobby.room.cleanup-interval:60000}")
    public void cleanupStaleRooms() {
        log.debug("Cleaning up stale rooms");

        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(idleTimeout);
        List<GameRoom> staleRooms = roomRepository.findStaleRooms(RoomStatus.WAITING, cutoff);

        staleRooms.forEach(room -> {
            log.info("Cleaning up stale room: {}", room.getRoomCode());
            deleteRoom(room.getRoomCode());
        });
    }

    private GameRoom getRoomByCode(String roomCode) {
        // Try cache first
        String roomKey = "room:" + roomCode;
        GameRoom room = (GameRoom) redisTemplate.opsForValue().get(roomKey);

        if (room == null) {
            // Try database
            room = roomRepository.findByRoomCode(roomCode)
                    .orElseThrow(() -> new RuntimeException("Room not found: " + roomCode));

            // Cache it
            redisTemplate.opsForValue().set(roomKey, room, idleTimeout, TimeUnit.SECONDS);
        }

        return room;
    }

    private void validateRoomForJoin(GameRoom room, JoinRoomRequest request) {
        if (room.getStatus() != RoomStatus.WAITING) {
            throw new RuntimeException("Game already in progress");
        }

        if (room.isPrivate() && !room.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("Invalid room password");
        }
    }

    private void updateRoomCache(GameRoom room) {
        String roomKey = "room:" + room.getRoomCode();
        redisTemplate.opsForValue().set(roomKey, room, idleTimeout, TimeUnit.SECONDS);
    }

    private void updateLobbyState(GameRoom room) {
        LobbyState state = lobbyStateRepository.findByRoomCode(room.getRoomCode())
                .orElse(new LobbyState());

        state.setRoomCode(room.getRoomCode());
        state.setStatus(room.getStatus());
        state.setPlayerCount(room.getPlayers().size());
        state.setReadyCount((int) room.getPlayers().stream()
                .filter(LobbyPlayer::isReady)
                .count());

        lobbyStateRepository.save(state);
    }

    private void broadcastLobbyUpdate(GameRoom room, EventType eventType, UUID updatedBy) {
        LobbyUpdateEvent event = LobbyUpdateEvent.builder()
                .eventType(eventType.name())
                .roomCode(room.getRoomCode())
                .status(room.getStatus())
                .players(room.getPlayers().stream()
                        .map(PlayerResponse::fromLobbyPlayer)
                        .collect(Collectors.toList()))
                .readyCount((int) room.getPlayers().stream()
                        .filter(LobbyPlayer::isReady)
                        .count())
                .updatedAt(updatedBy)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/lobby/" + room.getRoomCode(), event);
    }

    private String generateUniqueRoomCode() {
        String code;
        do {
            code = RandomStringUtils.randomAlphanumeric(roomCodeLength).toUpperCase();
        } while (roomRepository.findByRoomCode(code).isPresent());

        return code;
    }
}