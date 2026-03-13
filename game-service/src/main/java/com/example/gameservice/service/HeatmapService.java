package com.example.gameservice.service;

import com.example.gameservice.dto.HeatmapResponse;
import com.example.gameservice.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class HeatmapService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final GameRepository gameRepository;

    private final Map<UUID, Map<Integer, AtomicInteger>> heatmapBuffer = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastBroadcastTime = new ConcurrentHashMap<>();

    private static final String HEATMAP_KEY_PREFIX = "heatmap:";
    private static final long BROADCAST_INTERVAL_MS = 1000; // Broadcast every second

    public void updateHeatmap(UUID gameId, int boxPosition, UUID playerId, String action) {
        String key = HEATMAP_KEY_PREFIX + gameId;
        String field = boxPosition + ":" + playerId;

        // Update Redis
        if ("enter".equals(action)) {
            redisTemplate.opsForHash().increment(key, field, 1);
            redisTemplate.expire(key, 1, TimeUnit.HOURS);

            // Update buffer for periodic broadcast
            heatmapBuffer
                    .computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(boxPosition, k -> new AtomicInteger(0))
                    .incrementAndGet();
        } else if ("leave".equals(action)) {
            // Can decrement if needed, but typically we keep counts
        }

        // Check if we should broadcast
        long now = System.currentTimeMillis();
        Long lastTime = lastBroadcastTime.get(gameId);

        if (lastTime == null || now - lastTime > BROADCAST_INTERVAL_MS) {
            broadcastHeatmapUpdate(gameId);
            lastBroadcastTime.put(gameId, now);
        }
    }

    public HeatmapResponse getHeatmapData(UUID gameId) {
        String key = HEATMAP_KEY_PREFIX + gameId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        // Aggregate by box position
        Map<Integer, Integer> boxCounts = new ConcurrentHashMap<>();
        Map<UUID, Map<Integer, Integer>> playerCounts = new ConcurrentHashMap<>();

        entries.forEach((field, value) -> {
            String[] parts = field.toString().split(":");
            int boxPosition = Integer.parseInt(parts[0]);
            UUID playerId = UUID.fromString(parts[1]);
            int count = ((Number) value).intValue();

            boxCounts.merge(boxPosition, count, Integer::sum);

            playerCounts
                    .computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                    .put(boxPosition, count);
        });

        // Calculate intensities (0-1)
        int maxCount = boxCounts.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        Map<Integer, Double> intensities = new ConcurrentHashMap<>();
        boxCounts.forEach((pos, count) -> {
            intensities.put(pos, (double) count / maxCount);
        });

        // Build response
        return HeatmapResponse.builder()
                .gameId(gameId)
                .boxHoverCounts(boxCounts)
                .boxIntensities(intensities)
                .totalHovers(boxCounts.values().stream().mapToInt(Integer::intValue).sum())
                .build();
    }

    private void broadcastHeatmapUpdate(UUID gameId) {
        Map<Integer, AtomicInteger> buffer = heatmapBuffer.remove(gameId);
        if (buffer == null || buffer.isEmpty()) {
            return;
        }

        // Convert to regular map for sending
        Map<Integer, Integer> updates = new ConcurrentHashMap<>();
        buffer.forEach((pos, count) -> updates.put(pos, count.get()));

        // Send via WebSocket
        messagingTemplate.convertAndSend("/topic/game/" + gameId + "/heatmap", updates);

        log.debug("Broadcast heatmap update for game {}: {}", gameId, updates);
    }

    @Scheduled(fixedRate = 5000)
    public void cleanupStaleHeatmaps() {
        // Periodic cleanup of broadcast times
        long cutoff = System.currentTimeMillis() - 60000; // 1 minute
        lastBroadcastTime.entrySet().removeIf(entry -> entry.getValue() < cutoff);
    }

    public void clearHeatmap(UUID gameId) {
        String key = HEATMAP_KEY_PREFIX + gameId;
        redisTemplate.delete(key);
        heatmapBuffer.remove(gameId);
        lastBroadcastTime.remove(gameId);
    }
}