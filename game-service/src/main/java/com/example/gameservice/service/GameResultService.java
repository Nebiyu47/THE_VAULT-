package com.example.gameservice.service;


import com.example.gameservice.model.*;
import com.example.gameservice.model.enums.BoxType;
import com.example.gameservice.repository.GameResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameResultService {

    private final GameResultRepository gameResultRepository;

    public GameResult calculateGameResult(Game game) {
        log.info("Calculating results for game: {}", game.getGameId());

        // Calculate player results
        List<PlayerResult> playerResults = calculatePlayerResults(game);

        // Find winner
        PlayerResult winner = playerResults.stream()
                .filter(PlayerResult::isWinner)
                .findFirst()
                .orElse(null);

        // Calculate statistics
        Map<String, Object> statistics = calculateStatistics(game, playerResults);

        // Calculate game duration
        int durationSeconds = (int) ChronoUnit.SECONDS.between(game.getStartTime(),
                game.getEndTime() != null ? game.getEndTime() : LocalDateTime.now());

        // Find trap victims
        List<UUID> trapVictims = findTrapVictims(game);

        // Build result
        GameResult result = GameResult.builder()
                .resultId(UUID.randomUUID())
                .gameId(game.getGameId())
                .roomCode(game.getRoomCode())
                .winnerId(winner != null ? winner.getPlayerId() : null)
                .winnerName(winner != null ? winner.getUsername() : null)
                .jackpotWon(game.getCurrentJackpot())
                .totalMoves(game.getMoves().size())
                .boxesOpened(game.getOpenedBoxes())
                .completedAt(LocalDateTime.now())
                .gameDurationSeconds(durationSeconds)
                .playerResults(playerResults)
                .trapVictims(trapVictims)
                .replayData(generateReplayData(game))
                .statistics(statistics)
                .build();

        return result;
    }

    public GameResult saveGameResult(GameResult result) {
        return gameResultRepository.save(result);
    }

    public GameResult getGameResult(UUID gameId) {
        return gameResultRepository.findByGameId(gameId)
                .orElseThrow(() -> new RuntimeException("Game result not found: " + gameId));
    }

    private List<PlayerResult> calculatePlayerResults(Game game) {
        Map<UUID, List<Move>> playerMoves = game.getMoves().stream()
                .collect(Collectors.groupingBy(Move::getPlayerId));

        List<PlayerResult> results = new ArrayList<>();

        for (GamePlayer player : game.getPlayers()) {
            List<Move> moves = playerMoves.getOrDefault(player.getPlayerId(), Collections.emptyList());

            int trapsTriggered = (int) moves.stream()
                    .filter(m -> m.getBoxType() == BoxType.TRAP)
                    .count();

            int jackpotsFound = (int) moves.stream()
                    .filter(m -> m.getBoxType() == BoxType.JACKPOT)
                    .count();

            int pointsEarned = moves.stream()
                    .mapToInt(Move::getPointsEarned)
                    .sum();

            PlayerResult result = PlayerResult.builder()
                    .playerId(player.getPlayerId())
                    .username(player.getUsername())
                    .boxesOpened(moves.size())
                    .pointsEarned(pointsEarned)
                    .trapsTriggered(trapsTriggered)
                    .jackpotsFound(jackpotsFound)
                    .isWinner(player.getPlayerId().equals(game.getWinnerId()))
                    .rank(player.getPlayerId().equals(game.getWinnerId()) ? 1 : 2)
                    .build();

            results.add(result);
        }

        // Sort by points earned
        results.sort((r1, r2) -> Integer.compare(r2.getPointsEarned(), r1.getPointsEarned()));

        // Assign ranks
        for (int i = 0; i < results.size(); i++) {
            results.get(i).setRank(i + 1);
        }

        return results;
    }

    private Map<String, Object> calculateStatistics(Game game, List<PlayerResult> playerResults) {
        Map<String, Object> stats = new HashMap<>();

        // Average points
        double avgPoints = playerResults.stream()
                .mapToInt(PlayerResult::getPointsEarned)
                .average()
                .orElse(0);

        // Most active player
        PlayerResult mostActive = playerResults.stream()
                .max(Comparator.comparingInt(PlayerResult::getBoxesOpened))
                .orElse(null);

        // Luckiest player (most jackpots)
        PlayerResult luckiest = playerResults.stream()
                .max(Comparator.comparingInt(PlayerResult::getJackpotsFound))
                .orElse(null);

        stats.put("averagePoints", avgPoints);
        stats.put("mostActivePlayer", mostActive != null ? mostActive.getUsername() : null);
        stats.put("mostActiveBoxes", mostActive != null ? mostActive.getBoxesOpened() : 0);
        stats.put("luckiestPlayer", luckiest != null ? luckiest.getUsername() : null);
        stats.put("luckiestJackpots", luckiest != null ? luckiest.getJackpotsFound() : 0);
        stats.put("totalJackpotsFound", playerResults.stream().mapToInt(PlayerResult::getJackpotsFound).sum());
        stats.put("totalTrapsTriggered", playerResults.stream().mapToInt(PlayerResult::getTrapsTriggered).sum());

        return stats;
    }

    private List<UUID> findTrapVictims(Game game) {
        return game.getMoves().stream()
                .filter(m -> m.getBoxType() == BoxType.TRAP)
                .map(Move::getPlayerId)
                .distinct()
                .collect(Collectors.toList());
    }

    private Map<String, Object> generateReplayData(Game game) {
        Map<String, Object> replay = new HashMap<>();

        replay.put("gameId", game.getGameId());
        replay.put("startTime", game.getStartTime());
        replay.put("endTime", game.getEndTime());
        replay.put("moves", game.getMoves().stream()
                .map(move -> Map.of(
                        "player", move.getPlayerName(),
                        "position", move.getPosition(),
                        "type", move.getBoxType(),
                        "timestamp", move.getTimestamp(),
                        "points", move.getPointsEarned()
                ))
                .collect(Collectors.toList()));
        replay.put("boxLayout", game.getBoxes().stream()
                .map(box -> Map.of(
                        "position", box.getPosition(),
                        "type", box.getType(),
                        "opened", box.isOpened(),
                        "openedBy", box.getOpenedBy()
                ))
                .collect(Collectors.toList()));

        return replay;
    }
}