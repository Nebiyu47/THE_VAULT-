package com.example.lobbyservice.service;

import com.example.lobbyservice.repository.LobbyStateRepository;
import com.example.lobbyservice.repository.PlayerRepository;
import com.example.lobbyservice.repository.RoomRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LobbyService {

    private final RoomRepository roomRepository;
    private final LobbyStateRepository lobbyStateRepository;
    private final PlayerRepository playerRepository;
    private final RedisTemplate<String,Object>redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final MatchmakingService matchmakingService;

}
